package top.dreamlike.panama.uring.nativelib.wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.helper.JemallocAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class IoUringCore implements AutoCloseable {

    protected static final LibUring libUring = Instance.LIB_URING;
    private static final Set<Integer> ioUringFds = new CopyOnWriteArraySet<>();
    private final static Logger log = LoggerFactory.getLogger(IoUringCore.class);
    private final IoUring internalRing;
    private final int cqeSize;

    private MemorySegment cqePtrArray;

    private KernelTime64Type kernelTime64Type;

    public IoUringCore(Consumer<IoUringParams> ioUringParamsFactory) {
        MemorySegment ioUringMemory = Instance.LIB_JEMALLOC.malloc(IoUring.LAYOUT.byteSize());
        MemorySegment ioUringParamMemory = Instance.LIB_JEMALLOC.malloc(IoUringParams.LAYOUT.byteSize());

        this.internalRing = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringMemory);
        IoUringParams ioUringParams = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringParamMemory);

        ioUringParamsFactory.accept(ioUringParams);
        try {
            int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
            if (initRes < 0) {
                Instance.LIB_JEMALLOC.free(ioUringMemory);
                throw new SyscallException(initRes);
            }
            int uringFd = this.internalRing.getRing_fd();
            ioUringFds.add(uringFd);
            this.cqeSize = ioUringParams.getCq_entries();
            this.cqePtrArray = JemallocAllocator.INSTANCE.allocate(ValueLayout.ADDRESS, cqeSize);
            this.kernelTime64Type = Instance.STRUCT_PROXY_GENERATOR.allocate(JemallocAllocator.INSTANCE, KernelTime64Type.class);
        } finally {
            Instance.LIB_JEMALLOC.free(ioUringParamMemory);
        }
    }

    public static Set<Integer> getIoUringFds() {
        return Set.copyOf(ioUringFds);
    }

    public static boolean haveInit(int mayUringFd) {
        return ioUringFds.contains(mayUringFd);
    }

    @Override
    public void close() throws Exception {
        Instance.LIB_URING.io_uring_queue_exit(internalRing);
        MemorySegment ioUringMemory = StructProxyGenerator.findMemorySegment(internalRing);
        Instance.LIB_JEMALLOC.free(ioUringMemory);
        Instance.LIB_JEMALLOC.free(cqePtrArray);
        MemorySegment kernelTime64Type = StructProxyGenerator.findMemorySegment(internalRing);
        Instance.LIB_JEMALLOC.free(kernelTime64Type);
    }

    public IoUring getInternalRing() {
        return internalRing;
    }

    public void submitAndWait(long durationNs) {
        log.debug("duration: {}", durationNs);
        if (durationNs == -1) {
            libUring.io_uring_submit_and_wait(internalRing, 1);
        } else {
            kernelTime64Type.setTv_sec(durationNs / 1000000000);
            kernelTime64Type.setTv_nsec(durationNs % 1000000000);
            libUring.io_uring_submit_and_wait_timeout(internalRing, cqePtrArray, 1, kernelTime64Type, null);
        }
    }

    public void submit() {
        libUring.io_uring_submit(internalRing);
    }

    public Optional<IoUringSqe> ioUringGetSqe(boolean flushIfExhausted) {
        IoUringSqe sqe = libUring.io_uring_get_sqe(internalRing);
        //fast_sqe
        if (sqe != null) {
            return Optional.of(sqe);
        }
        if (!flushIfExhausted) {
            return Optional.empty();
        }
        while (sqe == null) {
            submit();
            sqe = libUring.io_uring_get_sqe(internalRing);
        }

        return Optional.of(sqe);
    }

    public final void processCqes(Consumer<IoUringCqe> nativeCqeConsumer) {
        int count = libUring.io_uring_peek_batch_cqe(internalRing, cqePtrArray, cqeSize);
        if (log.isDebugEnabled()) {
            log.debug("processCqes count:{}", count);
        }
        for (int i = 0; i < count; i++) {
            IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(cqePtrArray.getAtIndex(ValueLayout.ADDRESS, i));
            nativeCqeConsumer.accept(nativeCqe);
        }
        libUring.io_uring_cq_advance(internalRing, count);
    }
}
