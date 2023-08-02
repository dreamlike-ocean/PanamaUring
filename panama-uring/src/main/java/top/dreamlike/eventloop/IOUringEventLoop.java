package top.dreamlike.eventloop;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.errno.errno_h;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class IOUringEventLoop extends BaseEventLoop implements AutoCloseable {

    final IOUring ioUring;

    private final AtomicLong autoSubmitDuration;
    private static final AtomicInteger atomicInteger = new AtomicInteger();

    private final AtomicBoolean start = new AtomicBoolean(false);

    public IOUringEventLoop(int ringSize, int autoBufferSize, long autoSubmitDuration) {
        super();
        ioUring = new IOUring(ringSize, autoBufferSize);
        setName("io-uring-eventloop-" + atomicInteger.getAndIncrement());
        this.autoSubmitDuration = new AtomicLong(autoSubmitDuration);
//        scheduleTask(this::autoFlushTask, Duration.ofMillis(autoSubmitDuration));
        // 直接投递 不用考虑线程安全问题 此时没有竞争
        timerTasks.offer(new TimerTask(this::autoFlushTask, System.currentTimeMillis() + autoSubmitDuration));
    }

    private void autoFlushTask() {
        flush();
        scheduleTask(this::autoFlushTask, Duration.ofMillis(autoSubmitDuration.get()));
    }


    public void setAutoSubmitDuration(long autoSubmitDuration) {
        this.autoSubmitDuration.set(autoSubmitDuration);
    }

    public void start() {
        if (start.compareAndSet(false, true)) {
            super.start();
        }
    }

    public void flush() {
        if (!inEventLoop()) {
            execute(this::flush);
            return;
        }
        ioUring.submit();
    }

    public void wakeup0() {
        ioUring.wakeup();
    }

    @Override
    protected void selectAndWait(long duration) {
        ioUring.waitComplete(duration);
    }


    public void shutdown() {
        close.compareAndSet(false, true);
    }

    public AsyncFile openFile(String path, int ops) {
        return new AsyncFile(path, this, ops);
    }

    public AsyncFile openFile(int fd) {
        return new AsyncFile(fd, this);
    }

    public AsyncServerSocket openServer(String host, int port) {
        return new AsyncServerSocket(this, host, port);
    }

    public AsyncWatchService openWatchService() {
        return new AsyncWatchService(this);
    }


    public AsyncSocket openSocket(String host, int port) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        return new AsyncSocket(address, this);
    }


    public CompletableFuture<Integer> registerEventFd() {
        return runOnEventLoop(ioUring::registerEventFd);
    }


    @Unsafe("并发问题，不推荐直接使用")
    public List<IOOpResult> submitAndWait(int max) {
        ioUring.submit();
        ioUring.waitComplete();
        return ioUring.batchGetCqe(max);
    }

    /**
     * 不校验任何fd是否属于这个eventLoop！
     *
     * @param ops 在这里面写各个对应的async op
     * @return ops返回的future代表的结果
     */
    @Unsafe("不校验任何fd是否属于这个eventLoop")
    public <T> CompletableFuture<T> submitLinkedOpUnsafe(Supplier<CompletableFuture<T>> ops) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runOnEventLoop(() -> {
            try {
                IOUring.startLinked.set(true);
                ops.get()
                        .whenComplete((r, t) -> {
                            if (t != null) {
                                future.completeExceptionally(t);
                            } else {
                                future.complete(r);
                            }
                        });

            } catch (Throwable e) {
                future.completeExceptionally(e);
            } finally {
                IOUring.startLinked.set(false);
            }
        });
        return future;
    }


    public <T> CompletableFuture<T> submitLinkedOpSafe(Supplier<CompletableFuture<T>> ops) {
        checkCaptureContainAsyncFd(ops);
        return submitLinkedOpUnsafe(ops);
    }

    public CompletableFuture<Integer> cancel(long userData, int flag, boolean needSummit) {
        return runOnEventLoop((promise) -> {
            boolean contain = ioUring.contain(userData);
            if (!contain) {
                promise.completeExceptionally(new IllegalStateException("no such user data"));
                return;
            }
            ioUring.prep_cancel(userData, flag, res -> {
                //errno_h.EALREADY 也算取消成功
                if (res >= 0) {
                    promise.complete(res);
                    return;
                }

                res = -res;
                if (res == errno_h.EALREADY) {
                    promise.complete(0);
                    return;
                }

                String errorMsg = switch (res) {
                    case errno_h.ENOENT -> "user_data could not be located";
                    case errno_h.EINVAL -> "One of the fields set in the SQE was invalid";
//                    case errno_h.EALREADY ->
//                            " The execution state of the request has progressed far enough that cancellation is no longer possible";
                    default -> NativeHelper.getErrorStr(res);
                };
                promise.completeExceptionally(new NativeCallException(errorMsg));
            });
            flush();
        });
    }
    public CompletableFuture<Integer> cancel(long userData, int flag) {
       return cancel(userData, flag,  false);
    }


    //要求全部捕获的参数都得是同一个eventloop才可以
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


    static {
        AccessHelper.fetchIOURing = loop -> loop.ioUring;
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    @Override
    protected void afterSelect() {
        ioUring.batchGetCqe(1024).forEach(IOOpResult::doCallBack);
    }

    @Override
    protected void close0() throws Exception {
        ioUring.close();
    }


    @Override
    protected void afterClose() {
        for (TimerTask task : timerTasks) {
            if (task.future != null) {
                task.future.cancel(true);
            }
        }
        //若当前的io uring关闭 可能会导致有些服务不能结束
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
    }
}
