package top.dreamlike.panama.uring.eventloop;

import io.github.dreamlike.unsafe.vthread.Poller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.JemallocAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

public final class VTIoUringEventLoop extends IoUringEventLoop {

    private static final Logger log = LoggerFactory.getLogger(VTIoUringEventLoop.class);
    private final EventFd cqeReadyEventFd;
    private MemorySegment cqeReadyMemory;

    public VTIoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory) {
        super(ioUringParamsFactory, (r) -> Thread.ofVirtual().name("IoUringEventLoop-VT-" + count.incrementAndGet()).unstarted(r));
        //poll的时候不一定有事件 也可能是单纯的超时了
        cqeReadyEventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);
        int sysCall = libUring.io_uring_register_eventfd(internalRing, cqeReadyEventFd.fd());
        if (sysCall < 0) {
            throw new SyscallException(sysCall);
        }
        cqeReadyMemory = JemallocAllocator.INSTANCE.allocate(ValueLayout.JAVA_LONG);
    }

    @Override
    protected void submitAndWait(long duration) {
        int cqeCount = libUring.io_uring_submit_and_wait(internalRing, 0);
        log.debug("cqeCount: {}, duration: {}", cqeCount, duration);
        if (cqeCount == 0) {
            Poller.poll(cqeReadyEventFd.fd(), Poller.POLLIN, duration == -1 ? 0 : duration, () -> !hasClosed.get());
            log.debug("end jdk poll");
            //清除事件
            cqeReadyEventFd.read(cqeReadyMemory, (int) ValueLayout.JAVA_LONG.byteSize());
            if (log.isDebugEnabled()) {
                log.debug("end poll: {}", cqeReadyMemory.get(ValueLayout.JAVA_LONG, 0));
            }
        }
    }

    @Override
    protected void releaseResource() {
        super.releaseResource();
        Instance.LIBC.close(cqeReadyEventFd.fd());
        Instance.LIB_JEMALLOC.free(cqeReadyMemory);
    }
}
