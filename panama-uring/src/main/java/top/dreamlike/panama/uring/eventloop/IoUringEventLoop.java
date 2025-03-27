package top.dreamlike.panama.uring.eventloop;

import org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibPoll;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufRingSetupResult;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.NativeIoUringBufRing;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.sync.fd.EventFd;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.thirdparty.colletion.IntObjectHashMap;
import top.dreamlike.panama.uring.thirdparty.colletion.IntObjectMap;
import top.dreamlike.panama.uring.thirdparty.colletion.LongObjectHashMap;
import top.dreamlike.panama.uring.thirdparty.colletion.LongObjectMap;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_ASYNC_CANCEL_ALL;

public sealed class IoUringEventLoop implements AutoCloseable, Executor, Runnable permits VTIoUringEventLoop, AbstractNettyBridgeEventLoop {

    protected static final AtomicInteger count = new AtomicInteger(0);
    protected static final LibUring libUring = Instance.LIB_URING;
    private final static Logger log = LoggerFactory.getLogger(IoUringEventLoop.class);

    static {
        PanamaUringSecret.findUring = (loop) -> loop.ioUringCore.getInternalRing();
        PanamaUringSecret.findIoUringCore = (loop) -> loop.ioUringCore;
    }

    protected final AtomicBoolean hasClosed;
    protected final Thread owner;
    protected final IoUring internalRing;
    protected final MemoryAllocator<? extends OwnershipMemory> memoryAllocator;
    private final EventFd wakeUpFd;
    private final MpscUnboundedArrayQueue<Runnable> taskQueue;
    private final AtomicLong tokenGenerator;
    private final LongObjectHashMap<IoUringCompletionCallBack> callBackMap;
    private final PriorityQueue<ScheduledTask> scheduledTasks;
    private final OwnershipMemory eventReadBuffer;
    protected IoUringCore ioUringCore;
    private Consumer<Throwable> exceptionHandler = (t) -> {
        log.error("Uncaught exception in event loop", t);
    };
    private boolean enableLink;
    private IntObjectMap<IoUringBufferRing> bufferRingMap;

    public IoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory) {
        this(ioUringParamsFactory, (r) -> {
            Thread thread = new Thread(r);
            thread.setName("IoUringEventLoop-" + count.incrementAndGet());
            return thread;
        }, MemoryAllocator.LIBC_MALLOC);
    }

    public IoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory, ThreadFactory factory) {
        this(ioUringParamsFactory, factory, MemoryAllocator.LIBC_MALLOC);
    }

    public IoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory, ThreadFactory factory, MemoryAllocator<? extends OwnershipMemory> allocator) {
        this.wakeUpFd = new EventFd(0, 0);
        log.debug("wakeupFd: {}", wakeUpFd);
        this.taskQueue = new MpscUnboundedArrayQueue<>(1024);
        this.hasClosed = new AtomicBoolean();
        this.memoryAllocator = allocator;
        this.tokenGenerator = new AtomicLong(Long.MIN_VALUE + 1);
        this.callBackMap = new LongObjectHashMap<>();
        this.scheduledTasks = new PriorityQueue<>();
        this.ioUringCore = new IoUringCore(ioUringParamsFactory);
        this.internalRing = ioUringCore.getInternalRing();
        this.owner = factory.newThread(this);
        this.bufferRingMap = new IntObjectHashMap<>();
        this.eventReadBuffer = memoryAllocator.allocateOwnerShipMemory(ValueLayout.JAVA_LONG.byteSize());
        if (needWakeUpFd()) {
            initWakeUpFdMultiShot();
        }
    }

    public void start() {
        owner.start();
    }

    protected boolean needWakeUpFd() {
        return true;
    }

    private void registerWakeUpFd(IoUringSqe sqe) {
        libUring.io_uring_prep_read(sqe, wakeUpFd.fd(), eventReadBuffer.resource(), (int) ValueLayout.JAVA_LONG.byteSize(), 0);
    }

    private void initWakeUpFdMultiShot() {
        if (!closed()) {
            asyncOperation(this::registerWakeUpFd, _ -> initWakeUpFdMultiShot());
        }
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
                submitAndWait(-1);
            } else if (nextTask.deadlineNanos >= System.nanoTime()) {
                long duration = nextTask.deadlineNanos - System.nanoTime();
                submitAndWait(duration);
            }
            ioUringCore.processCqes(this::processCqes);
        }

        releaseResource();
    }

    protected void submitAndWait(long duration) {
        ioUringCore.submitAndWait(duration);
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
        if (Thread.currentThread() == owner) {
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
            OwnershipMemory resPtr = null;
            try {
                resPtr = memoryAllocator.allocateOwnerShipMemory(ValueLayout.JAVA_LONG.byteSize());
                NativeIoUringBufRing bufRing = libUring.io_uring_setup_buf_ring(internalRing, internalEntries, bufferGroupId, 0, resPtr.resource());
                int res = resPtr.resource().get(ValueLayout.JAVA_INT, 0);
                if (res < 0) {
                    return new IoUringBufRingSetupResult(res, null);
                }
                InternalNativeIoUringRing bufferRing = new InternalNativeIoUringRing(bufRing, bufferGroupId, internalEntries, blockSize, memoryAllocator);
                bufferRingMap.put(bufferGroupId, bufferRing);
                return new IoUringBufRingSetupResult(res, bufferRing);
            } finally {
                if (resPtr != null) {
                    resPtr.drop();
                }
            }
        });
    }

    public CompletableFuture<Optional<IoUringBufferRing>> findBufferRing(short bufferGroupId) {
        return runOnEventLoop(() -> Optional.ofNullable(bufferRingMap.get(bufferGroupId)));
    }

    public CancelableFuture<Integer> poll(PollableFd fd, int pollMask) {
        return (CancelableFuture<Integer>) asyncOperation(sqe -> {
            int registerFd = -1;
            if ((pollMask & LibPoll.POLLIN) != 0) {
                registerFd = fd.readFd();
            } else if ((pollMask & LibPoll.POLLOUT) != 0) {
                registerFd = fd.writeFd();
            } else {
                registerFd = fd.fd();
            }
            libUring.io_uring_prep_poll_add(sqe, registerFd, pollMask);
        }).thenApply(IoUringCqe::getRes);
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

    public CancelableFuture<IoUringCqe> sendMessage(IoUringEventLoop peer, int message, Consumer<IoUringCqe> peerMessageHandle) {
        long peerToken = peer.tokenGenerator.getAndIncrement();
        long token = tokenGenerator.getAndIncrement();
        IoUringCancelToken cancelToken = new IoUringCancelToken(token);
        CancelableFuture<IoUringCqe> promise = new CancelableFuture<>(cancelToken);

        peer.runOnEventLoop(() -> {
            peer.callBackMap.put(peerToken, new IoUringCompletionCallBack(internalRing.getRing_fd(), IoUringConstant.Opcode.IORING_OP_MSG_RING, peerMessageHandle));
            IoUringCancelToken realToken = (IoUringCancelToken) asyncOperation(
                    sqe -> libUring.io_uring_prep_msg_ring(sqe, peer.internalRing.getRing_fd(), message, peerToken, 0),
                    promise::complete
            );
            cancelToken.token = realToken.token;
        });

        return promise;
    }

    public CancelableFuture<IoUringCqe> sendMessage(int otherRingFd, int message, long userData) {
        //这里判断是为了防止干扰到IoUringEventLoop的userData到callback的机制
        if (IoUringCore.haveInit(otherRingFd)) {
            throw new IllegalArgumentException("please use sendMessage(IoUringEventLoop peer, int message, Consumer<IoUringCqe> peerMessageHandle)");
        }

        return asyncOperation(sqe -> libUring.io_uring_prep_msg_ring(sqe, otherRingFd, message, userData, 0));
    }

    public MemoryAllocator<? extends OwnershipMemory> getMemoryAllocator() {
        return memoryAllocator;
    }

    public CancelToken asyncOperation(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> repeatableCallback) {
        return asyncOperation(sqeFunction, repeatableCallback, false);
    }

    public CancelToken asyncOperation(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> repeatableCallback, boolean neeSubmit) {
        if (closed()) {
            throw new IllegalStateException("event loop is closed");
        }
        long token = tokenGenerator.getAndIncrement();
        IoUringCancelToken cancelToken = new IoUringCancelToken(token);
        Runnable r = () -> {
            IoUringSqe sqe = ioUringCore.ioUringGetSqe(true).get();
            sqeFunction.accept(sqe);

            if (NativeHelper.enableOpVersionCheck && sqe.getOpcode() > IoUringCore.PROBE.getLastOp()) {
                Instance.LIB_URING.io_uring_back_sqe(internalRing);
                throw new UnsupportedOperationException(sqe.getOpcode() + " is unsupported");
            }

            log.debug("sqe op: {}", sqe.getOpcode());

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
            afterFill(sqe);
            if (neeSubmit) {
                flush();
            }
        };
        if (inEventLoop()) {
            runWithCatchException(r);
        } else {
            execute(r);
        }
        return cancelToken;
    }

    protected void afterFill(IoUringSqe nativeSqe) {
    }

    public boolean isVirtual() {
        return owner.isVirtual();
    }

    public void flush() {
        if (inEventLoop()) {
            ioUringCore.submit();
        } else {
            execute(this::flush);
        }
    }

    protected final void processCqes(IoUringCqe nativeCqe) {
        long token = nativeCqe.getUser_data();
        boolean multiShot = nativeCqe.hasMore();
        IoUringCompletionCallBack callback = multiShot ? callBackMap.get(token) : callBackMap.remove(token);
        if (callback != null && callback.userCallBack != null) {
            if (log.isDebugEnabled()) {
                log.debug("IoUringCompletionCallBack: {}", callback);
            }
            runWithCatchException(() -> callback.userCallBack.accept(nativeCqe));
        }

    }

    @Override
    public void close() throws Exception {
        if (hasClosed.compareAndSet(false, true)) {
            log.debug("close wakeup!");
            wakeup();
        }
        join();
    }

    public boolean closed() {
        return hasClosed.get();
    }

    protected void releaseResource() {
        try {
            for (LongObjectMap.PrimitiveEntry<IoUringCompletionCallBack> entry : callBackMap.entries()) {
                long userData = entry.key();
                IoUringCompletionCallBack callBack = entry.value();
                IoUringCqe fakeCqe = new IoUringCqe();
                fakeCqe.setRes(-Libc.Error_H.ECANCELED);
                fakeCqe.setUser_data(userData);
                callBack.userCallBack.accept(fakeCqe);
            }
        } finally {
            this.wakeUpFd.close();
            eventReadBuffer.drop();
            try {
                ioUringCore.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean inEventLoop() {
        return Thread.currentThread() == owner;
    }

    public void join() throws InterruptedException {
        owner.join();
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
        wakeUpFd.eventfdWrite(1);
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
        static {
            PanamaUringSecret.peekCancelToken = (r) -> ((IoUringCancelToken) r).token;
        }

        volatile long token;
        AtomicReference<CompletableFuture<Integer>> cancelPromise = new AtomicReference<>();

        public IoUringCancelToken(long token) {
            this.token = token;
        }

        @Override
        public CompletableFuture<Integer> cancel() {
            return ioUringCancel(true);
        }

        @Override
        public CompletableFuture<Integer> ioUringCancel(boolean needSubmit) {
            if (isCancelled()) {
                return cancelPromise.get();
            }

            CompletableFuture<Integer> promise = new CompletableFuture<>();
            boolean hasCancel = cancelPromise.compareAndSet(null, promise);
            if (!hasCancel) {
                return cancelPromise.get();
            }
            asyncOperation(
                    sqe -> Instance.LIB_URING.io_uring_prep_cancel64(sqe, token, IORING_ASYNC_CANCEL_ALL),
                    cqe -> promise.complete(cqe.getRes()),
                    needSubmit
            );
            return promise;
        }

        @Override
        public boolean isCancelled() {
            return cancelPromise.get() != null;
        }
    }

    private class InternalNativeIoUringRing implements IoUringBufferRing {

        private final NativeIoUringBufRing internal;
        private final short bufferGroupId;
        private final OwnershipMemory[] buffers;
        private final MemoryAllocator<? extends OwnershipMemory> allocator;
        private final int count;
        private final int blockSize;
        private final AtomicBoolean autoFill;
        private boolean hasRelease = false;

        private InternalNativeIoUringRing(NativeIoUringBufRing internal, short bufferGroupId, int count, int blockSize, MemoryAllocator<? extends OwnershipMemory> allocator) {
            this.internal = internal;
            this.bufferGroupId = bufferGroupId;
            this.blockSize = blockSize;
            this.count = count;
            this.allocator = allocator;
            this.buffers = new OwnershipMemory[count];
            this.autoFill = new AtomicBoolean(true);
            //todo 暂时全部填充
            fillEmptyBuffer();
        }

        public OwnershipMemory removeBuffer(int bid) {
            if (bid > count || bid < 0) {
                throw new IllegalArgumentException("bid must in (0," + count + ")");
            }
            OwnershipMemory buffer = buffers[bid];
            buffers[bid] = null;
            if (buffer != null && autoFill.get()) {
                fillBuffer((short) bid);
            }
            return buffer;
        }

        @Override
        public IoUringEventLoop owner() {
            return IoUringEventLoop.this;
        }

        @Override
        public void fillSqe(IoUringSqe sqe) {
            assertClose();
            //我们不校验是否有元素可以使用 直接填充
            sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
            sqe.setBufGroup(bufferGroupId);
        }

        private int fillEmptyBuffer() {
            int count = 0;
            for (short i = 0; i < buffers.length; i++) {
                if (buffers[i] == null) {
                    OwnershipMemory memory = allocator.allocateOwnerShipMemory(blockSize);
                    buffers[i] = memory;
                    Instance.LIB_URING.io_uring_buf_ring_add(internal, memory.resource(), blockSize, i, libUring.io_uring_buf_ring_mask(buffers.length), count);
                    count++;
                }
            }

            if (count > 0) {
                Instance.LIB_URING.io_uring_buf_ring_advance(internal, count);
            }
            return count;
        }

        private void fillBuffer(short bid) {
            OwnershipMemory memory = allocator.allocateOwnerShipMemory(blockSize);
            Instance.LIB_URING.io_uring_buf_ring_add(internal, memory.resource(), blockSize, bid, libUring.io_uring_buf_ring_mask(buffers.length), 0);
            buffers[bid] = memory;
            Instance.LIB_URING.io_uring_buf_ring_advance(internal, 1);
        }

        private void assertClose() {
            if (hasRelease) {
                throw new IllegalStateException("The buffer ring has been released");
            }
        }

        @Override
        public int head() {
            MemorySegment ptr = Instance.LIBC_MALLOC.mallocNoInit(ValueLayout.JAVA_SHORT.byteSize());
            try {
                int res = Instance.LIB_URING.io_uring_buf_ring_head(internalRing, bufferGroupId, ptr);
                if (res < 0) {
                    throw new SyscallException(res);
                }
                return ptr.get(ValueLayout.JAVA_SHORT, 0L);
            } finally {
                Instance.LIBC_MALLOC.free(ptr);
            }
        }

        @Override
        public CompletableFuture<Integer> fillAll() {
            return runOnEventLoop(this::fillEmptyBuffer);
        }

        @Override
        public void setAutoFill(boolean autoFill) {
            this.autoFill.set(autoFill);
        }

        @Override
        public CompletableFuture<Void> releaseRing() {
            assertClose();
            return runOnEventLoop(() -> {
                assertClose();
                Instance.LIB_URING.io_uring_free_buf_ring(internalRing, internal, count, bufferGroupId);
                hasRelease = true;
                for (int i = 0; i < buffers.length; i++) {
                    var ioUringBufferRingElement = buffers[i];
                    if (ioUringBufferRingElement != null) {
                        ioUringBufferRingElement.drop();
                    }
                }
                return null;
            });
        }

        @Override
        public short getBufferGroupId() {
            return bufferGroupId;
        }
    }

}
