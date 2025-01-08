# Panama uring Java

这是一个探索性质的项目，使用Java的[新ffi机制](https://openjdk.org/jeps/424)为Java引入io_uring。

主要目的在于提供一个与基础read/write原语签名类似的 **Linux异步文件I/O** API，以补齐虚拟线程读写文件会导致pin住载体线程的短板，同时也提供了异步socket api。

本项目来自于[liburing](https://github.com/axboe/liburing)的启发，使用Java重新实现liburing并进行高阶封装

maven坐标为

```xml
<dependency>
    <groupId>io.github.dreamlike-ocean</groupId>
    <artifactId>panama-uring</artifactId>
    <version>${lastest}</version>
</dependency>
```

```shell
git clone https://github.com/dreamlike-ocean/PanamaUring.git
```

测试运行 由于依赖了[ClassFile api](https://openjdk.org/jeps/484)所以需要使用jdk24

```shell
mvn clean test -am -pl panama-uring
```

### 支持的特性

**注意**：若无提及则均为one shot模式

#### AsyncFile

- [x] 异步读取
- [x] 异步写入
- [x] IOSQE_BUFFER_SELECT模式的异步读取
- [x] 异步fsync

#### AsyncSocket

- [x] 异步connect （ipv4，ipv6，uds）
- [x] IOSQE_BUFFER_SELECT模式的异步recv
- [x] 异步write/send
- [x] mutlishot异步recv
- [x] zc tx支持

#### AsyncServerSocket

- [x] 异步accept
- [x] multishot的异步accept

#### IO_Uring

- [x] 任意数量的IOSQE_IO_LINK
- [x] 异步版本的splice api以及基于此的异步版本sendfile
- [x] 内存安全的cancel实现

#### 其余的异步操作

- [x] 异步的文件监听 inotify
- [x] 异步的eventfd读写
- [x] 异步的pipefd
- [x] 异步的Poll操作
- [x] 异步的madvise操作

#### 其他Native封装

- [x] 完整的Epoll绑定
- [x] 完整的eventFd绑定
- [x] 完整的unistd绑定
- [x] mmap绑定

#### 其余小玩意

- [x] Panama FFI的声明式运行时绑定 [点我看文档](./panama-generator/README.md)
- [x] 对于kotlin coroutine的支持，支持取消情况下的内存安全
- [x] kotlin coroutine情况下支持socket先进行poll后进行recv操作

### 构建/运行指南

下面本人使用的构建工具以及运行环境：

- Maven 3.8.4
- OpenJDK 24
- Linux >= 5.10 越新越好

构建非常简单

```shell
mvn clean package -DskipTests
```

### 设计细节

#### 所有权

我在代码中引入了OwnershipResource这样一个类表示某个api会“拿走”这个资源的所有权，然后在异步操作之后“归还”给调用者，设计思路来自于[monoio的实践](https://github.com/bytedance/monoio/blob/master/docs/zh/why-async-rent.md)

以异步read为例子,调用这个api时会把调用者的Buffer“拿走”，在io_uring内部获取到cqe之后再交还给用户

在大部分情况下，如果io_uring的cqe告诉我们出现错误（res为负）那么CancelableFuture这个future仍是success的但是BufferResult中result为负，仍旧会把这个buffer的所有权“归还”给用户

``` java
    default CancelableFuture<BufferResult<OwnershipMemory>> asyncRead(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<BufferResult<OwnershipMemory>>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()))
                .whenComplete(buffer::DropWhenException);
    }
```

#### 取消

Buffer的所有权只能在CancelableFuture完成后才能被“归还”给用户，如果想要做一个超时取消机制，那么就会遇到一些内存安全问题，问题来自于io_uring的取消机制是尽力取消，你的取消和异步操作本质上是并发的

试想这样一个场景，内核正在写入buffer但是你推送了一个取消操作就认为取消成功了，没有查看取消结果，那么就出现了数据读取丢失的问题，甚至你直接把这块buffer复用了就又叠加一个竞态问题

所以正常的取消应该

``` java
    var cancelableReadFuture = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
    eventLoop.submitScheduleTask(1, TimeUnit.SECONDS, () -> {
        cancelableReadFuture.ioUringCancel(true)
                .thenAccept(count -> Assert.assertEquals(1L, (long) count));
    });
    Integer res = cancelableReadFuture.get().syscallRes();
    if(Libc.Error_H.ECANCELED == -res) {
       //处理资源销毁等
    } else {
       //处理取消失败 事件已经完成的情况
    }
```

#### 线程模型

由于io_uring无锁sq和cq特性，所以在处理sqe填充和提交时最好是一个原子操作，所以这里按照常规的IO设计，对应的sqe操作会调度到对应的EventLoop上进行操作

举个例子

```java
    public CancelToken asyncOperation(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> repeatableCallback, boolean neeSubmit) {
    Runnable r = fillSqeFunction;
    if (inEventLoop()) {
        runWithCatchException(r);
    } else {
        execute(r);
    }
    return cancelToken;
}
```

#### wakeup

当EventLoop阻塞在wait cqe时，我们想要在任意线程里面唤醒它，最容易想到的就是使用 `IORING_OP_NOP`这个OP， 但是这个也存在我们之前说的并发问题。

所以抄了一把jdk的selector实现，我会让io_uring监听一个eventfd，需要唤醒时我们只要增加一个eventfd的计数即可

> 注意这里的eventfd只是一个普通的fd，并非使用`io_uring_register_eventfd`这个方法来监听cqe完成事件的

wakeup的实现核心就这些。

然后我们来谈谈细节：

由于只支持one shot模式的监听，即submit一次sqe只会产生一次可读事件

所以需要读取到事件之后 再注册一次监听，类似于一个“异步递归”

```java
private void multiShotReadEventfd() {
    prep_read(wakeUpFd.getFd(), 0, wakeUpReadBuffer, (__) -> {
        // 轮询到了直接再注册一个可读事件
        wakeUpFd.read(wakeUpReadBuffer);
        multiShotReadEventfd();
    });
    submit();
}
```

#### IOSQE_IO_LINK

这里要保证两点

- 原子化——EventLoop机制保证
- 保证范围内的fd都属于同一个io_uring实现


```java
    public void testLinked() throws Exception {
    IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
        params.setSq_entries(4);
        params.setFlags(0);
    });
    ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
    try (eventLoop) {
        IoUringNoOp ioUringNoOp = new IoUringNoOp(eventLoop);
        eventLoop.start();
        eventLoop.linkedScope(() -> {
            var tmp = ioUringNoOp;
            AtomicReference<IoUringSqe> t = new AtomicReference<>();
            eventLoop.asyncOperation(sqe -> {
                Instance.LIB_URING.io_uring_prep_nop(sqe);
                t.set(sqe);
            }).thenAccept(cqe -> queue.add(cqe.getRes()));
            Assert.assertTrue(t.get().isLinked());

            eventLoop.asyncOperation(sqe -> {
                Instance.LIB_URING.io_uring_prep_nop(sqe);
                t.set(sqe);
            }).thenAccept(cqe -> queue.add(cqe.getRes()));
            Assert.assertTrue(t.get().isLinked());
        }, () -> {
            AtomicReference<IoUringSqe> t = new AtomicReference<>();
            eventLoop.asyncOperation(sqe -> {
                Instance.LIB_URING.io_uring_prep_nop(sqe);
                t.set(sqe);
            }).thenAccept(cqe -> queue.add(cqe.getRes()));
            Assert.assertFalse(t.get().isLinked());
        });
    }
    eventLoop.join();
    Assert.assertEquals(3, queue.size());
    for (Integer i : queue) {
        Assert.assertEquals(Integer.valueOf(0), i);
    }
}
```

还要支持捕获任意数量的IoUringOperator实现类

这里使用了一个针对于lambda实现的小技巧 当然你也可以通过top.dreamlike.panama.uring.skipSameEventLoopCheck这个Property关掉检查（默认关闭）

```java
    public static boolean inSameEventLoop(IoUringEventLoop eventLoop, Object o) {
    if (isSkipSameEventLoopCheck) {
        return true;
    }

    Class<?> aClass = o.getClass();
    for (Field field : aClass.getDeclaredFields()) {
        if (!IoUringOperator.class.isAssignableFrom(field.getType())) {
            continue;
        }
        field.setAccessible(true);
        var loop = ((IoUringOperator) LambdaHelper.runWithThrowable(() -> field.get(o))).owner();
        if (loop != eventLoop) {
            return false;
        }
    }
    return true;
}
```

#### 虚拟线程支持

当前做了一个特殊的实现 `VTIoUringEventLoop` 其使用虚拟线程作为EventLoop实现，原理如下：

首先java虚拟线程底层有一组read poller来不断poll事件然后恢复对应线程执行，那么我们实际上就可以把一些fd直接挂到这个poller上，请它来poll一些fd,已知iouring发布cqe时支持自动向某个eventfd发送消息（io_uring_register_eventfd） 那么我就可以把这个eventfd挂到jdk的poller上 然后开启一个虚拟线程跑对应的EventLoop，还可以复用对应的ForkJoin线程池来处理cqe回调和各种操作，直接去“借用”JDK内部的虚拟线程池化算力

具体的Poll实现可以参考 `sun.nio.ch.Poller::poll` 是一个内部的静态方法，我通过一些hack的手段强行打开的

#### Netty支持

当前也做了对Netty Epoll和io_uring transport的支持，可以将PanamaUring的EventLoop挂在netty的EventLoop上

其核心原理与虚拟线程的支持一致，都为evenfd + poller，而netty的poller实现是通过借用Netty的4.2的ioHandle与ioHandler模型实现的