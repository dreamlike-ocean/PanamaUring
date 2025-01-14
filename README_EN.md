# Panama uring Java

This is an exploratory project that uses Java's new FFI mechanism to introduce io_uring to Java.

The main purpose is to provide a Linux asynchronous file I/O API similar to the signature of the basic read/write syscall, in order to make up for the shortcoming that virtual thread reading and writing files will pin the carrier thread, and also provide asynchronous socket api.

This project is inspired by [liburing](https://github.com/axboe/liburing), using Java to reimplement liburing and perform high-level encapsulation

The maven coordinates are

```xml
<dependency>
    <groupId>io.github.dreamlike-ocean</groupId>
    <artifactId>panama-uring</artifactId>
    <version>${lastest}</version>
</dependency>
```

Run Tests.Since it depends on the [ClassFile api](https://openjdk.org/jeps/484), it needs to use jdk23 and enable the `--enable-preview`  (it is expected that it can be removed after jdk24 is released)

```shell
mvn clean test -am -pl panama-uring
```

### Supported features
**Note:** If not mentioned, it is all one shot mode

#### AsyncFile
- [x] Asynchronous read
- [x] Asynchronous write
- [x] Asynchronous read with IOSQE_BUFFER_SELECT mode
- [x] Asynchronous fsync
#### AsyncSocket
- [x] Asynchronous connect (IPv4, IPv6, UDS)
- [x] Asynchronous receive with IOSQE_BUFFER_SELECT mode
- [x] Asynchronous write/send
- [x] Asynchronous Multi-shot receive
- [x] Support for zero-copy transmit (zc tx)
#### AsyncServerSocket
- [x] Asynchronous accept
- [x] Multi-shot asynchronous accept
#### IO_Uring
- [x] Arbitrary number of IOSQE_IO_LINK
- [x] Asynchronous splice API and asynchronous sendfile based on it
- [x] Memory-safe cancellation implementation
#### Other Asynchronous Operations
- [x] Asynchronous file monitoring (inotify)
- [x] Asynchronous eventfd read/write
- [x] Asynchronous pipefd
- [x] Asynchronous poll
- [x] Asynchronous madvise
#### Other Native Wrappers
- [x] Full Epoll binding
- [x] Full eventfd binding
- [x] Full unistd binding
- [x] mmap binding
#### Additional Features
- [x] Declarative runtime bindings with Panama FFI Documentation
- [x] Support for Kotlin coroutines, ensuring memory safety in cancellation scenarios
- [x] Support for polling sockets before receiving in Kotlin coroutine scenarios

### Build/Run Guide
Here is the setup and environment used:

- Maven 3.8.4
- OpenJDK 23
- Linux ≥ 5.11 (newer versions recommended)

To build the project:

```shell
mvn clean package -DskipTests
```

### Design Details

#### Ownership

The code introduces an OwnershipResource class to represent the "ownership" of a resource taken by an API. After an asynchronous operation, ownership is "returned" to the caller. The design is inspired by [monoio's approach.](https://github.com/bytedance/monoio/blob/master/docs/en/why-async-rent.md)

Take asynchronous read as an example, when calling this API, it will "take away" the caller's Buffer, and return it to the user after the io_uring internal obtains the cqe

In most cases, if the cqe of io_uring tells us that an error has occurred (res is negative), then the CancelableFuture future is still successful, but the result in BufferResult is negative, and the ownership of the buffer will still be "returned" to the user

Example:
```java
default CancelableFuture<BufferResult<OwnershipMemory>> asyncRead(OwnershipMemory buffer, int len, int offset) {
    return (CancelableFuture<BufferResult<OwnershipMemory>>) owner()
        .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
        .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()))
        .whenComplete(buffer::DropWhenException);
}
```
#### Cancellation

The ownership of the Buffer can only be "returned" to the user after the CancelableFuture is completed. If you want to implement a timeout cancellation mechanism, you will encounter some memory safety issues. The problem comes from the fact that the cancellation mechanism of io_uring is to cancel as much as possible, and your cancellation and asynchronous operations are essentially concurrent.

Imagine such a scenario, the kernel is writing to the buffer but you push a cancel operation and consider the cancellation successful without checking the cancellation result, then there is a problem of data loss, and even if you directly reuse this buffer, there is another race condition.
So the normal cancellation should be

```java
    var cancelableReadFuture = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
    eventLoop.submitScheduleTask(1, TimeUnit.SECONDS, () -> {
        cancelableReadFuture.ioUringCancel(true)
               .thenAccept(count -> Assert.assertEquals(1L, (long) count));
    });
    Integer res = cancelableReadFuture.get().syscallRes();
    if(Libc.Error_H.ECANCELED == -res) {
       //Handle resource destruction, etc.
    } else {
       //Handle cancellation failure The event has been completed
    }
```

#### Thread model

Due to the lock-free sq and cq characteristics of io_uring, it is best to perform sqe filling and submission as an atomic operation, so here is a conventional IO design, and the corresponding sqe operation will be scheduled to the corresponding EventLoop for operation
For example

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

hen the EventLoop blocks on waiting for cqe, we want to wake it up in any thread. The easiest way to think of is to use the IORING_OP_NOP operation, but this also has concurrency issues we mentioned earlier.
So I copied the selector implementation of jdk, I will let io_uring listen to an eventfd, and when we need to wake up, we just need to increase the count of an eventfd.The core of the wakeup implementation is these.

> That the eventfd here is just an ordinary fd, not using the io_uring_register_eventfd method to listen to cqe completion events.

Then let's talk about the details:
Since only one shot mode of listening is supported, that is, submitting an sqe will only generate one readable event.
So after reading the event, you need to register the listening again, similar to an "asynchronous recursion"

```java
private void multiShotReadEventfd() {
    prep_read(wakeUpFd.getFd(), 0, wakeUpReadBuffer, (__) -> {
        // After polling, register a readable event again
        wakeUpFd.read(wakeUpReadBuffer);
        multiShotReadEventfd();
    });
    submit();
}
```

#### IOSQE_IO_LINK
Here are two points to ensure:
- Atomicity - Guaranteed by the EventLoop mechanism
- Ensure that all fds in the scope belong to the same io_uring implementation

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

It is also necessary to support the capture of any number of IoUringOperator implementation classes.
Here is a small trick for lambda implementation. Of course, you can also turn off the check through the top.dreamlike.panama.uring.skipSameEventLoopCheck property (off by default)

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

#### Virtual thread support

The current implementation of a special `VTIoUringEventLoop` implementation used virtual threads as the EventLoop. 
The principle is as follows:
First, the Java virtual thread has a set of read pollers to continuously poll events and then resume the execution of the corresponding thread. Then we can actually hang some fds on this poller and ask it to poll some fds. It is known that iouring can automatically send a message to a certain eventfd when it issues a cqe (io_uring_register_eventfd). Then we can register this eventfd on the jdk's poller, then start a virtual thread to run the corresponding EventLoop, and can also reuse the corresponding ForkJoin thread pool to handle cqe callbacks and various operations, directly "borrowing" the virtual thread pooling computing power inside the JDK.
The specific Poll implementation can refer to sun.nio.ch.Poller::poll.

#### Netty support

Currently, support for Netty Epoll and io_uring transport has also been implemented, which can hang PanamaUring's EventLoop on netty's EventLoop.
The core principle is the same as that of virtual thread support, which is evenfd + poller, while netty's poller implementation is achieved by borrowing Netty's 4.2 ioHandle and ioHandler models.