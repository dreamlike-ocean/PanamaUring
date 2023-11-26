# Panama uring Java

这是一个探索性质的项目，使用Java的[新ffi](https://openjdk.org/jeps/424)为Java引入io_uring。

主要目的在于提供一个与基础read/write原语签名类似的 **Linux异步文件I/O** API，以补齐虚拟线程读写文件会导致pin住载体线程的短板。

这个项目并非与netty的io_uring一样会从系统调用开始处理，而是直接修改[liburing](https://github.com/axboe/liburing)
源码，在此基础上封装调用，即它最底层只是一层对liburing的封装。

**目前阶段参考价值大于实用价值，<del>在jdk21之后我会做进一步的API适配</del>,(垃圾java毁我人生，这东西jdk21稳定不了)，Panama
API仍在变动之中**

由于依赖了liburing的代码所以需要一并clone liburing仓库

```shell
git clone --recurse-submodules https://github.com/dreamlike-ocean/PanamaUring.git
```

### 支持的特性

**注意**：若无提及则均为one shot模式

#### AsyncFile

- [x] 异步读取
- [x] 异步写入
- [x] IOSQE_BUFFER_SELECT模式的异步读取
- [x] 异步fsync
- [x] 通过flock和定时任务实现的文件锁

#### AsyncSocket

- [x] 异步connect （ipv4，ipv6）
- [x] IOSQE_BUFFER_SELECT模式的异步recv
- [x] 异步write
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
- [x] 基于Epoll的Async socket read,Async socket write,Async socket connect,Async socket accept

#### 其他Native封装

- [x] 完整的Epoll绑定
- [x] 完整的eventFd绑定
- [x] 完整的unistd绑定

#### 其余小玩意

- [x] 基于EventFd将IO Uring与Epoll连接在一起,socket api走Epoll驱动,收割cqe也由Epoll驱动,等价于Epoll监听IO Uring

### 构建/运行指南

初衷就是最小依赖，所以只依赖于jctools和slfj4，前者用于EventLoop的task queue，后者则是日志门面。jctools可以替换为juc的BlockingQueue的任意实现。

下面本人使用的构建工具以及运行环境：

> 注意 jdk版本不能升级也不能降级，Panama api可能不一致

- Maven 3.8.4
- OpenJDK 21
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
public CompletableFuture<byte[]> readSelected(int offset, int length) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    eventLoop.runOnEventLoop(() -> {
        if (!uring.prep_selected_read(fd, offset, length, future)) {
            future.completeExceptionally(new Exception("没有空闲的sqe"));
        }
    });
    return future;
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

比如说 asyncFile1和asyncFile2都必须是同一个io_uring

```java
eventLoop.submitLinkedOpSafe(() -> {
            asyncFile1.read();
            asyncFile2.write();
        });
```

还要支持捕获任意数量的`AsyncFile`/`AsyncSocket`/`AsyncServerSocket`

这里使用了一个针对于lambda实现的小技巧

```java
private boolean checkCaptureContainAsyncFd(Object ops) {
    try {
        for (Field field : ops.getClass().getDeclaredFields()) {
            if (!AsyncFd.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            IOUringEventLoop eventLoop = ((AsyncFd) field.get(ops)).fetchEventLoop();
            if (eventLoop != this) {
                return false;
            }
        }
    } catch (Throwable t) {
        throw new AssertionError("should not reach here", t);
    }
        return true;
        }
```

#### Epoll

为什么这里面会有Epoll？

因为最早的实现是，网络IO走epoll不变，文件IO走io_uring，epoll监听io_uring cqe完成事件的eventfd，当epoll轮询到这个eventfd时再去收割cqe。

但是目前我完整实现了这个EventLoop请看[EpollUringEventLoop](panama-uring/src/main/java/top/dreamlike/eventloop/EpollUringEventLoop.java)
,同时提供了基于Epoll的socket基础操作
