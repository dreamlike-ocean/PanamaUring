package top.dreamlike.panama.uring.nativelib.wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.helper.Unsafe;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.OSIoUringProbe;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Unsafe("only single Thread")
public class IoUringCore implements AutoCloseable {

    protected static final LibUring libUring = Instance.LIB_URING;
    private static final Set<Integer> ioUringFds = new CopyOnWriteArraySet<>();
    private final static Logger log = LoggerFactory.getLogger(IoUringCore.class);
    private final IoUring internalRing;
    private final int cqeSize;

    private final KernelTime64Type kernelTime64Type;

    public static final OSIoUringProbe PROBE = new OSIoUringProbe();

    public IoUringCore(Consumer<IoUringParams> ioUringParamsFactory) {
        MemorySegment ioUringMemory = Instance.LIBC_MALLOC.malloc(IoUring.LAYOUT.byteSize());
        MemorySegment ioUringParamMemory = Instance.LIBC_MALLOC.malloc(IoUringParams.LAYOUT.byteSize());

        this.internalRing = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringMemory);
        IoUringParams ioUringParams = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringParamMemory);

        ioUringParamsFactory.accept(ioUringParams);
        try {
            int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
            if (initRes < 0) {
                Instance.LIBC_MALLOC.free(ioUringMemory);
                throw new SyscallException(initRes);
            }
            int uringFd = this.internalRing.getRing_fd();
            ioUringFds.add(uringFd);
            this.cqeSize = ioUringParams.getCq_entries();
            this.kernelTime64Type = Instance.STRUCT_PROXY_GENERATOR.enhance(
                    Instance.LIBC_MALLOC.malloc(KernelTime64Type.LAYOUT.byteSize())
            );
        } finally {
            Instance.LIBC_MALLOC.free(ioUringParamMemory);
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
        int ringFd = internalRing.getRing_fd();
        Instance.LIB_URING.io_uring_queue_exit(internalRing);
        MemorySegment ioUringMemory = StructProxyGenerator.findMemorySegment(internalRing);
        Instance.LIBC_MALLOC.free(ioUringMemory);
        MemorySegment kernelTime64Type = StructProxyGenerator.findMemorySegment(this.kernelTime64Type);
        Instance.LIBC_MALLOC.free(kernelTime64Type);
        ioUringFds.remove(ringFd);
    }

    public IoUring getInternalRing() {
        return internalRing;
    }

    public int submitAndWait(long durationNs) {
        log.debug("duration: {}", durationNs);
        int sqeCount;
        if (durationNs == -1) {
            sqeCount = libUring.io_uring_submit_and_wait(internalRing, 1);
        } else {
            kernelTime64Type.setTv_sec(durationNs / 1000000000);
            kernelTime64Type.setTv_nsec(durationNs % 1000000000);
            sqeCount = libUring.io_uring_submit_and_wait_timeout(internalRing, 1, kernelTime64Type);
        }
        return sqeCount;
    }

    public int submit() {
        return libUring.io_uring_submit(internalRing);
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

    public final int countReadyCqe() {
        return libUring.io_uring_cq_ready(internalRing);
    }

    public final void processCqes(Consumer<IoUringCqe> nativeCqeConsumer) {
        processCqes(nativeCqeConsumer, true);
    }

    public final void processCqes(Consumer<IoUringCqe> nativeCqeConsumer, boolean advance) {
        long[] cqes = libUring.io_uring_peek_batch_cqe(internalRing, cqeSize);
        int count = cqes.length;
        if (log.isDebugEnabled()) {
            log.debug("processCqes count:{}", count);
        }
        for (int i = 0; i < count; i++) {
            IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(MemorySegment.ofAddress(cqes[i]).reinterpret(Long.MAX_VALUE));
            nativeCqeConsumer.accept(nativeCqe);
        }
        if (advance) {
            ioUringCqAdvance(count);
        }
    }

    public final void ioUringCqAdvance(int count) {
        libUring.io_uring_cq_advance(internalRing, count);
    }

    public final List<IoUringCqe> processCqes() {
        ArrayList<IoUringCqe> list = new ArrayList<>();
        processCqes(list::add, false);
        return list;
    }

    static {
        PanamaUringSecret.getCqSizeFromCore = c -> c.cqeSize;
    }
}
