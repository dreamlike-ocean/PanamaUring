package top.dreamlike.panama.uring.eventloop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jctools.queues.MpscUnboundedArrayQueue;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.helper.OSIoUringProbe;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.*;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;
import top.dreamlike.panama.uring.sync.fd.EventFd;
import top.dreamlike.panama.uring.thirdparty.colletion.LongObjectHashMap;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.BitSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_ASYNC_CANCEL_ALL;

public class IoUringEventLoop extends Thread implements AutoCloseable, Executor {

    private static final OSIoUringProbe PROBE = new OSIoUringProbe();

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final Logger log = LogManager.getLogger(IoUringEventLoop.class);

    private static final LibUring libUring = Instance.LIB_URING;
    private static final int cqeSize = 4;
    private final EventFd wakeUpFd;
    private final AtomicBoolean inWait;
    private final AtomicBoolean hasClosed;

    private IoUring internalRing;

    private Arena singleThreadArena;

    private final MpscUnboundedArrayQueue<Runnable> taskQueue;

    private MemorySegment cqePtrs;

    private final AtomicLong tokenGenerator;

    private final LongObjectHashMap<IoUringCompletionCallBack> callBackMap;

    private final PriorityQueue<ScheduledTask> scheduledTasks;

    private KernelTime64Type kernelTime64Type;

    private MemorySegment eventReadBuffer;

    private Consumer<Throwable> exceptionHandler = (t) -> {
        log.error("Uncaught exception in event loop", t);
    };

    private boolean enableLink;

    public IoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory) {
        this.wakeUpFd = new EventFd(0, 0);
        this.inWait = new AtomicBoolean(false);
        this.taskQueue = new MpscUnboundedArrayQueue<>(1024);
        this.hasClosed = new AtomicBoolean();
        this.tokenGenerator = new AtomicLong(0);
        this.callBackMap = new LongObjectHashMap<>();
        this.taskQueue.add(() -> initRing(ioUringParamsFactory));
        this.scheduledTasks = new PriorityQueue<>();
        setName("IoUringEventLoop-" + count.getAndIncrement());
    }

    private void initRing(Consumer<IoUringParams> ioUringParamsFactory) {
        this.singleThreadArena = Arena.ofConfined();
        this.internalRing = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, IoUring.class);
        IoUringParams ioUringParams = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, IoUringParams.class);
        ioUringParamsFactory.accept(ioUringParams);
        int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
        if (initRes < 0) {
            throw new SyscallException(initRes);
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
            while (true) {
                ScheduledTask next = scheduledTasks.peek();
                if (next != null && next.deadlineNanos <= System.nanoTime()) {
                    taskQueue.offer(scheduledTasks.poll().task);
                } else {
                    break;
                }
            }
            while (!taskQueue.isEmpty()) {
                runWithCatchException(taskQueue.poll());
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
            inWait.set(true);
            processCqes();
        }
        releaseResource();
    }

    private void runWithCatchException(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            exceptionHandler.accept(t);
        }
    }

    public void setExceptionHandler(Consumer<Throwable> handler) {
        Objects.requireNonNull(handler);
        this.exceptionHandler = handler;
    }

    public void enableLink() {
        runOnEventLoop(() -> this.enableLink = true);
    }

    private void disableLink() {
        runOnEventLoop(() -> this.enableLink = false);
    }

    public <V> CompletableFuture<V> runOnEventLoop(Supplier<V> callable) {
        CompletableFuture<V> future = new CompletableFuture<>();
        runOnEventLoop(() -> {
            future.complete(callable.get());
        });
        return future;
    }

    public void runOnEventLoop(Runnable runnable) {
        if (Thread.currentThread() == this) {
            runWithCatchException(runnable);
        } else {
            execute(runnable);
        }
    }

    /**
     * @param entries       entries 是缓冲区环中请求的条目数。该参数的大小必须是 2 的幂。
     * @param bufferGroupId bgid 是所选的缓冲区组 ID
     */
    public CompletableFuture<IoUringBufRingSetupResult> setupBufferRing(int entries, int blockSize, short bufferGroupId) {
        if (entries <= 0) {
            throw new IllegalArgumentException("entries must be greater than 0");
        }
        int MAXIMUM_CAPACITY = 1 << 30;
        //该参数的大小必须是 2 的幂。
        entries = -1 >>> Integer.numberOfLeadingZeros(entries - 1);
        entries = (entries < 0) ? 1 : (entries >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : entries + 1;
        int internalEntries = entries;
        return runOnEventLoop(() -> {
            MemorySegment resPtr = null;
            try {
                resPtr = Instance.LIB_JEMALLOC.malloc(ValueLayout.JAVA_LONG.byteSize());
                NativeIoUringBufRing bufRing = libUring.io_uring_setup_buf_ring(internalRing, internalEntries, bufferGroupId, 0, resPtr);
                InternalNativeIoUringRing bufferRing = new InternalNativeIoUringRing(bufRing, bufferGroupId, internalEntries, blockSize);
                return new IoUringBufRingSetupResult(resPtr.get(ValueLayout.JAVA_INT, 0), bufferRing);
            } finally {
                if (resPtr != null) {
                    Instance.LIB_JEMALLOC.free(resPtr);
                }
            }
        });
    }

    private CancelToken fillTemplate(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> callback) {
        return fillTemplate(sqeFunction, callback, false);
    }

    private CancelToken fillTemplate(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> callback, boolean needSubmit) {
        long token = tokenGenerator.getAndIncrement();
        Runnable r = () -> {
            IoUringSqe sqe = ioUringGetSqe();
            sqeFunction.accept(sqe);
            if (NativeHelper.enableOpVersionCheck && sqe.getOpcode() > PROBE.getLastOp()) {
               Instance.LIB_URING.io_uring_back_sqe(internalRing);
               throw new UnsupportedOperationException(sqe.getOpcode() + " is unsupported");
            }
            sqe.setUser_data(token);
            callBackMap.put(token, new IoUringCompletionCallBack(sqe.getFd(), sqe.getOpcode(), callback));
            if (needSubmit) {
                flush();
            }
        };
        if (inEventLoop()) {
            runWithCatchException(r);
        } else {
            execute(r);
        }
        return new IoUringCancelToken(token);
    }

    public CancelableFuture<IoUringCqe> asyncOperation(Consumer<IoUringSqe> sqeFunction) {
        IoUringCancelToken cancelToken = new IoUringCancelToken(0);
        CancelableFuture<IoUringCqe> promise = new CancelableFuture<>(cancelToken);
        CancelToken realToken = asyncOperation(sqeFunction, promise::complete);
        cancelToken.token = ((IoUringCancelToken) realToken).token;
        return promise;
    }

    public void linkedScope(Runnable linkedScopeFunction, Runnable lastFunction) {
        NativeHelper.inSameEventLoop(this, linkedScopeFunction);
        NativeHelper.inSameEventLoop(this, lastFunction);
        runOnEventLoop(() -> {
            enableLink();
            linkedScopeFunction.run();
            disableLink();
            lastFunction.run();
        });
    }

    public CancelToken asyncOperation(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> repeatableCallback) {
        long token = tokenGenerator.getAndIncrement();
        IoUringCancelToken cancelToken = new IoUringCancelToken(token);
        Runnable r = () -> {
            IoUringSqe sqe = ioUringGetSqe();
            sqeFunction.accept(sqe);

            if (NativeHelper.enableOpVersionCheck && sqe.getOpcode() > PROBE.getLastOp()) {
                Instance.LIB_URING.io_uring_back_sqe(internalRing);
                throw new UnsupportedOperationException(sqe.getOpcode() + " is unsupported");
            }

            sqe.setUser_data(token);
            if (enableLink) {
                sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_IO_LINK));
            }
            callBackMap.put(token, new IoUringCompletionCallBack(sqe.getFd(), sqe.getOpcode(), cqe -> {
                IoUringCqe uringCqe = new IoUringCqe();
                uringCqe.setFlags(cqe.getFlags());
                uringCqe.setRes(cqe.getRes());
                uringCqe.setUser_data(cqe.getUser_data());
                repeatableCallback.accept(uringCqe);
            }));
        };
        if (inEventLoop()) {
            runWithCatchException(r);
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
        if (log.isDebugEnabled()) {
            log.info("processCqes count:{}", count);
        }
        for (int i = 0; i < count; i++) {
            IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(cqePtrs.getAtIndex(ValueLayout.ADDRESS, i));
            long token = nativeCqe.getUser_data();
            boolean multiShot = nativeCqe.hasMore();
            IoUringCompletionCallBack callback = multiShot ? callBackMap.get(token) : callBackMap.remove(token);
            if (callback != null && callback.userCallBack != null) {
                runWithCatchException(() -> callback.userCallBack.accept(nativeCqe));
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

    public CompletableFuture<Void> timeout(long delay, TimeUnit timeUnit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        submitScheduleTask(delay, timeUnit, () -> future.complete(null));
        return future;
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

        static {
            PanamaUringSecret.peekCancelToken = (r) -> ((IoUringCancelToken) r).token;
        }
    }

    private class InternalNativeIoUringRing implements IoUringBufferRing {
        private final NativeIoUringBufRing internal;
        private final short bufferGroupId;
        private final OwnershipMemory base;
        private final int count;
        private final int blockSize;
        private final int mask;

        private boolean hasRelease = false;

        private final BitSet occupySet;

        private InternalNativeIoUringRing(NativeIoUringBufRing internal, short bufferGroupId, int count, int blockSize) {
            this.internal = internal;
            this.bufferGroupId = bufferGroupId;
            this.blockSize = blockSize;
            this.count = count;
            this.mask = Instance.LIB_URING.io_uring_buf_ring_mask(count);
            try (OwnershipMemory bufPtrPtr = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.ADDRESS.byteSize())) {
                base = Instance.LIB_JEMALLOC.posixMemalign(bufPtrPtr.resource(), 4096, (long) blockSize * count);
                MemorySegment blockBase = base.resource();
                for (int i = 0; i < count; i++) {
                    Instance.LIB_URING.io_uring_buf_ring_add(internal, blockBase, blockSize, (short) (i + 1), mask, i);
                    blockBase = MemorySegment.ofAddress(blockBase.address() + blockSize);
                }
                Instance.LIB_URING.io_uring_buf_ring_advance(internal, count);
                occupySet = new BitSet(count);
            } catch (Throwable t) {
                log.error("release memory failure!", t);
                throw new IllegalStateException(t);
            }
        }

        public IoUringBufferRingElement getMemoryByBid(int bid) {
            if (bid > count || bid < 0) {
                throw new IllegalArgumentException("bid must in (0," + count + ")");
            }
            assertClose();
            MemorySegment element = MemorySegment.ofAddress(base.resource().address() + (long) (bid - 1) * blockSize).reinterpret(blockSize);
            return new IoUringBufferRingElement(this, bid, element, occupySet.get(bid - 1));
        }

        public CompletableFuture<IoUringBufferRingElement> removeBuffer(int bid) {
            return runOnEventLoop(() -> {
                if (bid > count || bid < 0) {
                    throw new IllegalArgumentException("bid must in (0," + count + ")");
                }
                occupySet.set(bid - 1);
                return getMemoryByBid(bid);
            });
        }

        public CompletableFuture<Void> releaseBuffer(IoUringBufferRingElement element) {
            return runOnEventLoop(() -> {
                if (element.ring() != this) {
                    throw new IllegalArgumentException("The element is not belong to this ring");
                }
                int bid = element.bid();
                if (!occupySet.get(bid - 1)) {
                    throw new IllegalArgumentException("The element has been released");
                }
                Instance.LIB_URING.io_uring_buf_ring_add(internal, element.element(), blockSize, (short) bid, mask, 0);
                Instance.LIB_URING.io_uring_buf_ring_advance(internal, 1);
                occupySet.clear(bid - 1);
                return null;
            });
        }

        @Override
        public IoUringEventLoop owner() {
            return IoUringEventLoop.this;
        }

        private void assertClose() {
            if (hasRelease) {
                throw new IllegalStateException("The buffer ring has been released");
            }
        }

        @Override
        public CompletableFuture<Void> releaseRing() {
            assertClose();
            return runOnEventLoop(() -> {
                assertClose();
                Instance.LIB_URING.io_uring_unregister_buf_ring(internalRing, bufferGroupId);
                hasRelease = true;
                Instance.LIB_JEMALLOC.free(base.resource());
                return null;
            });
        }

        //是否还有余量
        public boolean hasAvailableElements() {
            return occupySet.cardinality() < count;
        }

        @Override
        public short getBufferGroupId() {
            return bufferGroupId;
        }

        static {
            PanamaUringSecret.peekOccupyBitSet = (r) -> ((InternalNativeIoUringRing) r).occupySet;
        }
    }

}
