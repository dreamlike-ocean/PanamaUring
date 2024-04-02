package top.dreamlike.panama.uring.eventloop;

import org.jctools.queues.MpscUnboundedArrayQueue;
import top.dreamlike.helper.StackValue;
import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.fd.EventFd;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.*;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;
import top.dreamlike.panama.uring.thirdparty.colletion.IntObjectHashMap;
import top.dreamlike.panama.uring.thirdparty.colletion.IntObjectMap;
import top.dreamlike.panama.uring.thirdparty.colletion.LongObjectHashMap;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_ASYNC_CANCEL_ALL;
import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_CQE_F_MORE;

public class IoUringEventLoop extends Thread implements AutoCloseable, Executor {
    private static final LibUring libUring = Instance.LIB_URING;
    private static final int cqeSize = 4;
    private final EventFd wakeUpFd;
    private final AtomicBoolean inWait;
    private final AtomicBoolean hasClosed;

    private IoUring internalRing;

    private Arena singleThreadArena;

    private MpscUnboundedArrayQueue<Runnable> taskQueue;

    private MemorySegment cqePtrs;

    private final AtomicLong tokenGenerator;

    private final LongObjectHashMap<IoUringCompletionCallBack> callBackMap;

    private final PriorityQueue<ScheduledTask> scheduledTasks;

    private KernelTime64Type kernelTime64Type;

    private MemorySegment eventReadBuffer;

    private static final VarHandle IO_URING_BUF_RING_HANDLER;

    private final IntObjectMap<IoUringBufRing> bufRingMap = new IntObjectHashMap<>();

    public IoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory) {
        this.wakeUpFd = new EventFd(0, 0);
        this.inWait = new AtomicBoolean(false);
        this.taskQueue = new MpscUnboundedArrayQueue<>(1024);
        this.hasClosed = new AtomicBoolean();
        this.tokenGenerator = new AtomicLong(0);
        this.callBackMap = new LongObjectHashMap<>();
        this.taskQueue.add(() -> initRing(ioUringParamsFactory));
        this.scheduledTasks = new PriorityQueue<>();
    }

    private void initRing(Consumer<IoUringParams> ioUringParamsFactory) {
        this.singleThreadArena = Arena.ofConfined();
        this.internalRing = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, IoUring.class);
        IoUringParams ioUringParams = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, IoUringParams.class);
        ioUringParamsFactory.accept(ioUringParams);
        int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
        if (initRes < 0) {
            throw new IllegalArgumentException(STR."io_uring_queue_init_params error,error Reason: \{DebugHelper.getErrorStr(-initRes)}");
        }
        this.cqePtrs = singleThreadArena.allocate(ValueLayout.ADDRESS, cqeSize);
        this.kernelTime64Type = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, KernelTime64Type.class);
        this.eventReadBuffer = singleThreadArena.allocate(ValueLayout.JAVA_LONG);
        initWakeUpFdMultiShot();
    }

    private void registerWakeUpFd(IoUringSqe sqe) {
        libUring.io_uring_prep_read(sqe, wakeUpFd.fd(), eventReadBuffer, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
    }

    private void initWakeUpFdMultiShot() {
        fillTemplate(this::registerWakeUpFd, _ -> initWakeUpFdMultiShot());
    }

    @Override
    public void run() {
        while (!hasClosed.get()) {
            inWait.set(true);
            while (true) {
                ScheduledTask next = scheduledTasks.peek();
                if (next != null && next.deadlineNanos <= System.nanoTime()) {
                    taskQueue.offer(scheduledTasks.poll().task);
                } else {
                    break;
                }
            }
            while (!taskQueue.isEmpty()) {
                taskQueue.poll().run();
            }
            ScheduledTask nextTask = scheduledTasks.peek();
            //如果没有任务或者下一个任务的时间大于当前时间，那么就直接提交
            if (nextTask == null) {
                inWait.set(false);
                libUring.io_uring_submit_and_wait(internalRing, 1);
            } else if (nextTask.deadlineNanos >= System.nanoTime()) {
                inWait.set(false);
                long duration = nextTask.deadlineNanos - System.nanoTime();
                kernelTime64Type.setTv_sec(duration / 1000000000);
                kernelTime64Type.setTv_nsec(duration % 1000000000);
                libUring.io_uring_submit_and_wait_timeout(internalRing, cqePtrs, cqeSize, kernelTime64Type, null);
            }

            processCqes();
        }
        releaseResource();
    }

    /**
     * @param entries       entries 是缓冲区环中请求的条目数。该参数的大小必须是 2 的幂。
     * @param bufferGroupId bgid 是所选的缓冲区组 ID
     */
    public CompletableFuture<IoUringBufRingSetupResult> setupBufferRing(int entries, int bufferGroupId) {
        if (entries <= 0) {
            throw new IllegalArgumentException("entries must be greater than 0");
        }
        int MAXIMUM_CAPACITY = 1 << 30;
        //该参数的大小必须是 2 的幂。
        entries = -1 >>> Integer.numberOfLeadingZeros(entries - 1);
        entries = (entries < 0) ? 1 : (entries >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : entries + 1;
        int internalEntries = entries;
        return CompletableFuture.supplyAsync(() -> {
            IoUringBufRing uringBufRing = bufRingMap.get(bufferGroupId);
            if (uringBufRing != null) {
                return new IoUringBufRingSetupResult(0, uringBufRing);
            }
            MemorySegment resPtr = null;
            try {
                resPtr = Instance.LIB_JEMALLOC.malloc(ValueLayout.JAVA_LONG.byteSize());
                IoUringBufRing bufRing = libUring.io_uring_setup_buf_ring(internalRing, internalEntries, bufferGroupId, 0, resPtr);
                IoUringBufRingSetupResult ringSetupResult = new IoUringBufRingSetupResult(resPtr.get(ValueLayout.JAVA_INT, 0), bufRing);
                if (bufRing != null) {
                    //回填EventLoop字段
                    IO_URING_BUF_RING_HANDLER.set(bufRing, this);
                    bufRingMap.put(bufferGroupId, ringSetupResult.bufRing());
                }
                return ringSetupResult;
            } finally {
                if (resPtr != null) {
                    Instance.LIB_JEMALLOC.free(resPtr);
                }
            }
        }, this);
    }

    private CancelToken fillTemplate(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> callback) {
        return fillTemplate(sqeFunction, callback, false);
    }

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

    public CancelableFuture<IoUringCqe> asyncOperation(Consumer<IoUringSqe> sqeFunction) {
        long token = tokenGenerator.getAndIncrement();
        IoUringCancelToken cancelToken = new IoUringCancelToken(token);
        CancelableFuture<IoUringCqe> promise = new CancelableFuture<>(cancelToken);
        Runnable r = () -> {
            IoUringSqe sqe = ioUringGetSqe();
            sqeFunction.accept(sqe);
            sqe.setUser_data(token);
            callBackMap.put(token, new IoUringCompletionCallBack(sqe.getFd(), sqe.getOpcode(), cqe -> {
                IoUringCqe uringCqe = new IoUringCqe();
                uringCqe.setFlags(cqe.getFlags());
                uringCqe.setRes(cqe.getRes());
                uringCqe.setUser_data(cqe.getUser_data());
                promise.complete(uringCqe);
            }));
        };
        if (inEventLoop()) {
            r.run();
        } else {
            execute(r);
        }
        return promise;
    }

    public CancelToken asyncOperation(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> repeatableCallback) {
        long token = tokenGenerator.getAndIncrement();
        IoUringCancelToken cancelToken = new IoUringCancelToken(token);
        Runnable r = () -> {
            IoUringSqe sqe = ioUringGetSqe();
            sqeFunction.accept(sqe);
            sqe.setUser_data(token);
            callBackMap.put(token, new IoUringCompletionCallBack(sqe.getFd(), sqe.getOpcode(), cqe -> {
                IoUringCqe uringCqe = new IoUringCqe();
                uringCqe.setFlags(cqe.getFlags());
                uringCqe.setRes(cqe.getRes());
                uringCqe.setUser_data(cqe.getUser_data());
                repeatableCallback.accept(uringCqe);
            }));
        };
        if (inEventLoop()) {
            r.run();
        } else {
            execute(r);
        }
        return cancelToken;
    }

    private IoUringSqe ioUringGetSqe() {
        IoUringSqe sqe = libUring.io_uring_get_sqe(internalRing);
        //fast_sqe
        if (sqe != null) {
            return sqe;
        }
        flush();
        return libUring.io_uring_get_sqe(internalRing);
    }

    public void flush() {
        if (inEventLoop()) {
            libUring.io_uring_submit(internalRing);
        } else {
            execute(this::flush);
        }
    }


    private void processCqes() {
        int count = libUring.io_uring_peek_batch_cqe(internalRing, cqePtrs, cqeSize);
        for (int i = 0; i < count; i++) {
            IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(cqePtrs.getAtIndex(ValueLayout.ADDRESS, i));
            long token = nativeCqe.getUser_data();
            boolean multiShot = nativeCqe.hasMore();
            IoUringCompletionCallBack callback = multiShot ? callBackMap.get(token) : callBackMap.remove(token);
            if (callback != null && callback.userCallBack != null) {
                callback.userCallBack.accept(nativeCqe);
            }
        }
        libUring.io_uring_cq_advance(internalRing, count);
    }


    @Override
    public void close() throws Exception {
        if (hasClosed.compareAndSet(false, true)) {
            wakeup();
        }
    }

    private void releaseResource() {
        this.wakeUpFd.close();
        Instance.LIB_URING.io_uring_queue_exit(internalRing);
        this.singleThreadArena.close();
        StackValue.release();
    }

    private boolean inEventLoop() {
        return Thread.currentThread() == this;
    }

    @Override
    public void execute(Runnable command) {
        if (hasClosed.get()) {
            throw new IllegalStateException("EventLoop has closed");
        }
        taskQueue.add(command);
        wakeup();
    }

    private void wakeup() {
        if (inWait.compareAndSet(false, true)) {
            wakeUpFd.eventfdWrite(1);
        }
    }

    public void submitScheduleTask(long delay, TimeUnit timeUnit, Runnable task) {
        long deadlineNanos = System.nanoTime() + timeUnit.toNanos(delay);
        execute(() -> scheduledTasks.offer(new ScheduledTask(deadlineNanos, task)));
    }

    private record ScheduledTask(long deadlineNanos, Runnable task) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask o) {
            return Long.compare(deadlineNanos, o.deadlineNanos);
        }
    }

    private record IoUringCompletionCallBack(int fd, byte op, Consumer<IoUringCqe> userCallBack) {
    }

    private class IoUringCancelToken implements CancelToken {
        long token;
        AtomicReference<CompletableFuture<Integer>> cancelPromise = new AtomicReference<>();


        public IoUringCancelToken(long token) {
            this.token = token;
        }

        @Override
        public CompletableFuture<Integer> cancel() {
            if (isCancelled()) {
                return cancelPromise.get();
            }

            CompletableFuture<Integer> promise = new CompletableFuture<>();
            boolean hasCancel = cancelPromise.compareAndSet(null, promise);
            if (!hasCancel) {
                return cancelPromise.get();
            }
            CancelToken _ = fillTemplate(sqe -> {
                Instance.LIB_URING.io_uring_prep_cancel64(sqe, token, IORING_ASYNC_CANCEL_ALL);
            }, cqe -> promise.complete(cqe.getRes()), true);
            return promise;
        }

        @Override
        public boolean isCancelled() {
            return cancelPromise.get() != null;
        }
    }

    static {
        try {
            IO_URING_BUF_RING_HANDLER = MethodHandles.privateLookupIn(IoUringBufRing.class, MethodHandles.lookup()).findVarHandle(IoUringBufRing.class, "owner", IoUringEventLoop.class)
                    .withInvokeExactBehavior();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
