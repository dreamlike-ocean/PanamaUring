package top.dreamlike.async.uring;

import org.jctools.queues.MpscLinkedQueue;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.helper.Unsafe;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class IOUringEventLoop extends Thread implements Executor {

    final IOUring ioUring;

    private final MpscLinkedQueue<Runnable> tasks;

    private final PriorityQueue<TimerTask> timerTasks;

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
        timerTasks = new PriorityQueue<>(Comparator.comparingLong(TimerTask::getDeadline));
        timerTasks.offer(new TimerTask(this::autoFlushTask, System.currentTimeMillis() + autoSubmitDuration));
        close = new AtomicBoolean(false);
        lastSubmitTimeStamp = System.currentTimeMillis();
    }

    private void autoFlushTask() {
        flush();
        timerTasks.offer(new TimerTask(this::autoFlushTask, System.currentTimeMillis() + autoSubmitDuration.get()));
    }


    public CompletableFuture<Void> scheduleTask(Runnable runnable, Duration duration) {
        CompletableFuture<Void> res = new CompletableFuture<>();
        runOnEventLoop(() -> {
            long millis = duration.toMillis();
            TimerTask task = new TimerTask(() -> {
                runnable.run();
                res.complete(null);
            }, System.currentTimeMillis() + millis);
            task.future = res;
            timerTasks.offer(task);
        });
        return res;
    }

    public void setAutoSubmitDuration(long autoSubmitDuration) {
        this.autoSubmitDuration.setRelease(autoSubmitDuration);
    }

    @Override
    public void run() {
        while (!close.get()) {
            long duration = autoSubmitDuration.get();
            TimerTask peek = timerTasks.peek();
            if (peek != null) {
                //过期任务
                if (peek.deadline <= System.currentTimeMillis()) {
                    runAllTimerTask();
                }
                //最近的一个定时任务
                duration = peek.deadline - System.currentTimeMillis();
            }
            ioUring.waitComplete(duration);
            wakeupFlag.setRelease(true);
            //到期的任务
            runAllTimerTask();
            ioUring.batchGetCqe(1024).forEach(IOOpResult::doCallBack);
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
        }
        try {
            ioUring.close();
        } catch (Exception e) {
            //ignore
        } finally {
            for (TimerTask task : timerTasks) {
                if (task.future != null) {
                    task.future.cancel(true);
                }
            }
            //若当前的io uring关闭 可能会导致有些服务不能结束
            tasks.forEach(Runnable::run);
        }
    }

    private void runAllTimerTask() {
        while (timerTasks.peek().deadline <= System.currentTimeMillis()) {
            TimerTask timerTask = timerTasks.poll();
            timerTask.runnable.run();
        }
    }

    public void start() {
        if (start.compareAndSet(false, true)) {
            super.start();
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

    public AsyncWatchService openWatchService() {
        return new AsyncWatchService(this);
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


    public <T> CompletableFuture<T> runOnEventLoop(Supplier<T> fn) {
        CompletableFuture<T> res = new CompletableFuture<>();
        runOnEventLoop(() -> {
            try {
                res.complete(fn.get());
            } catch (Exception e) {
                res.completeExceptionally(e);
            }
        });
        return res;
    }


    static {
        AccessHelper.fetchIOURing = loop -> loop.ioUring;
    }


    private class TimerTask {
        public Runnable runnable;
        //绝对值 ms
        public long deadline;

        public CompletableFuture future;

        public TimerTask(Runnable runnable, long deadline) {
            this.runnable = runnable;
            this.deadline = deadline;
        }

        public long getDeadline() {
            return deadline;
        }
    }

}
