# Panama uring Java

这是一个探索性质的项目，使用Java的[新ffi](https://openjdk.org/jeps/424)为Java引入io_uring。

主要目的在于提供一个与基础read/write原语签名类似的 **Linux异步文件I/O** API，以补齐虚拟线程读写文件会导致pin住载体线程的短板，同时也提供了异步socket api。

这个项目并非与netty的io_uring一样会从系统调用开始处理，而是直接使用[liburing](https://github.com/axboe/liburing)
源码，在此基础上封装调用，即它最底层只是一层对liburing的封装。

maven坐标为

```xml
<dependency>
    <groupId>io.github.dreamlike-ocean</groupId>
    <artifactId>panama-uring</artifactId>
    <version>${lastest}</version>
    <classifier>linux-x86_64</classifier>
</dependency>
```

```shell
git clone https://github.com/dreamlike-ocean/PanamaUring.git
```

测试运行 因为依赖了一些预览特性 所以需要使用最新的jdk

```shell
mvn clean test -am
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

#### AsyncServerSocket

- [x] 异步accept
- [x] multishot的异步accept

#### IO_Uring

- [x] 任意数量的IOSQE_IO_LINK
- [x] 异步版本的splice api以及基于此的异步版本sendfile

#### 其余的异步fd

- [x] 异步的文件监听 inotify
- [x] 异步的eventfd读写
- [x] 异步的pipefd

#### 其他Native封装

- [x] 完整的Epoll绑定
- [x] 完整的eventFd绑定
- [x] 完整的unistd绑定

#### 其余小玩意

- [x] Panama FFI的声明式运行时绑定 [点我看文档](./panama-generator/README.md)

### 构建/运行指南

下面本人使用的构建工具以及运行环境：

- Maven 3.8.4
- OpenJDK 22
- Linux >= 5.10
- makefile GNU Make 4.3

构建非常简单

```shell
mvn clean package -DskipTests
```

### 设计细节

#### 线程模型

由于io_uring的双环特性，其实推荐单线程获取sqe，然后同一线程再submit。

目前获取io_uring实例直接使用默认参数获取，所以使用的是`io_uring_get_sqe`这个函数获取sqe，这个方法会导致从环上摘下一个sqe，而且封装的EventLoop带有一个定期submit的功能，所以要求`io_uring_get_sqe`和sqe参数填充这多个操作必须是原子的

进而需要把这几个操作打包为一个操作切换到EventLoop上面执行

举个例子

```java
    private CancelToken fillTemplate(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> callback, boolean needSubmit) {
    long token = tokenGenerator.getAndIncrement();
    Runnable r = () -> {
        IoUringSqe sqe = ioUringGetSqe();
        sqeFunction.accept(sqe);
        sqe.setUser_data(token);
        callBackMap.put(token, new IoUringCompletionCallBack(sqe.getFd(), sqe.getOpcode(), callback));
        if (needSubmit) {
            flush();
        }
    };
    if (inEventLoop()) {
        r.run();
    } else {
        execute(r);
    }
    return new IoUringCancelToken(token);
}
```

#### wakeup

当EventLoop阻塞在wait cqe时，我们想要在任意线程里面唤醒它，最容易想到的就是使用 `IORING_OP_NOP`这个OP， 但是这个也存在我们之前说的并发问题。

所以抄了一把jdk的selector实现，我会让io_uring监听一个eventfd，需要唤醒时我们只要增加一个eventfd的计数即可

> 注意这里的eventfd只是一个普通的fd，并非使用`io_uring_register_eventfd`这个方法来监听cqe完成事件的

wakeup的实现核心就这些。

然后我们来谈谈细节：

由于只支持one shot模式的监听，即submit一次sqe只会产生一次可读事件

所以需要读取到事件之后 再注册一次监听

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

这里使用了一个针对于lambda实现的小技巧

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