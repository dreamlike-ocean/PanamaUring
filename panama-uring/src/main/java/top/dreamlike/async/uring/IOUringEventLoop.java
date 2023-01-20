package top.dreamlike.async.uring;

import org.jctools.queues.MpscLinkedQueue;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.helper.Unsafe;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class IOUringEventLoop extends Thread implements Executor {

    final IOUring ioUring;

    private final MpscLinkedQueue<Runnable> tasks;

    private final Thread worker;

    private final AtomicLong autoSubmitDuration;

    private final AtomicBoolean close;

    private long lastSubmitTimeStamp;

    private static final AtomicInteger atomicInteger = new AtomicInteger();

    private final AtomicBoolean start = new AtomicBoolean(false);

    private final AtomicBoolean wakeupFlag = new AtomicBoolean(true);

    //todo jdk20之后更换为ScopeLocal实现
    public static final ThreadLocal<Boolean> startLinked = ThreadLocal.withInitial(() -> false);

    public IOUringEventLoop(int ringSize, int autoBufferSize, long autoSubmitDuration) {
        ioUring = new IOUring(ringSize, autoBufferSize);
        tasks = new MpscLinkedQueue<>();
        setName("io-uring-eventloop-" + atomicInteger.getAndIncrement());
        worker = this;
        this.autoSubmitDuration = new AtomicLong(autoSubmitDuration);
        close = new AtomicBoolean(false);
        lastSubmitTimeStamp = System.currentTimeMillis();
    }

    public void setAutoSubmitDuration(long autoSubmitDuration) {
        this.autoSubmitDuration.setRelease(autoSubmitDuration);
    }

    @Override
    public void run() {
        while (!close.get()) {
            long duration = autoSubmitDuration.getAcquire();
            ioUring.waitComplete(duration);
            wakeupFlag.setRelease(true);
            if (duration != -1 && System.currentTimeMillis() - lastSubmitTimeStamp > duration) {
                flush();
            }
            ioUring.batchGetCqe(1024).forEach(IOOpResult::doCallBack);
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
        }
        try {
            ioUring.close();
        } catch (Exception e) {
            //ignore
        }
    }

    public void start() {
        if (start.compareAndSet(false, true)) {
            worker.start();
        }
    }

    public void flush() {
        Thread currentThread = Thread.currentThread();
        if (currentThread != worker) {
            execute(this::flush);
            return;
        }
        lastSubmitTimeStamp = System.currentTimeMillis();
        ioUring.submit();
    }

    public void wakeup() {
        if (!wakeupFlag.compareAndSet(false, true)) {
            return;
        }
        ioUring.wakeup();
    }

    @Override
    public void execute(Runnable command) {
        tasks.offer(command);
        wakeup();
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


    public AsyncSocket openSocket(String host, int port) {
        InetSocketAddress address = new InetSocketAddress(host, port);
        return new AsyncSocket(address, this);
    }

    @Unsafe("并发问题，不推荐直接使用")
    public List<IOOpResult> submitAndWait(int max) {
        ioUring.submit();
        ioUring.waitComplete();
        return ioUring.batchGetCqe(max);
    }

    public boolean inEventLoop() {
        return Thread.currentThread() == this;
    }

    /**
     * 不校验任何fd是否属于这个eventLoop！
     *
     * @param ops 在这里面写各个对应的async op
     * @param <T>
     * @return ops返回的future代表的结果
     */
    @Unsafe("不校验任何fd是否属于这个eventLoop")
    public <T> CompletableFuture<T> submitLinkedOpUnsafe(Supplier<CompletableFuture<T>> ops) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runOnEventLoop(() -> {
            try {
                startLinked.set(true);
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
                startLinked.set(false);
            }
        });
        return future;
    }


    public <T> CompletableFuture<T> submitLinkedOpSafe(Supplier<CompletableFuture<T>> ops) {
        checkCaptureContainAsyncFd(ops);
        return submitLinkedOpUnsafe(ops);
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

    public boolean isStartLinked() {
        return startLinked.get();
    }


    public void runOnEventLoop(Runnable runnable) {
        if (inEventLoop()) {
            runnable.run();
            return;
        }
        execute(runnable);
    }

    static {
        AccessHelper.fetchIOURing = loop -> loop.ioUring;
    }



}
