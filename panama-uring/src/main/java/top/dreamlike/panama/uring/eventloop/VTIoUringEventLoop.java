package top.dreamlike.panama.uring.eventloop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.unsafe.TrustedLookup;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.sync.fd.EventFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class VTIoUringEventLoop extends IoUringEventLoop {

    private static final Logger log = LoggerFactory.getLogger(VTIoUringEventLoop.class);
    private final EventFd cqeReadyEventFd;
    private final OwnershipMemory cqeReadyMemory;

    public VTIoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory) {
        super(ioUringParamsFactory, (r) -> Thread.ofVirtual().name("IoUringEventLoop-VT-" + count.incrementAndGet()).unstarted(r));
        //poll的时候不一定有事件 也可能是单纯的超时了
        cqeReadyEventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);
        int sysCall = libUring.io_uring_register_eventfd(internalRing, cqeReadyEventFd.fd());
        if (sysCall < 0) {
            throw new SyscallException(sysCall);
        }
        cqeReadyMemory = memoryAllocator.allocateOwnerShipMemory(ValueLayout.JAVA_LONG.byteSize());
    }

    @Override
    protected void submitAndWait(long duration) {
        int cqeCount = libUring.io_uring_submit_and_wait(internalRing, 0);
        log.debug("cqeCount: {}, duration: {}", cqeCount, duration);
        if (cqeCount == 0) {
            Poller.poll(cqeReadyEventFd.fd(), Poller.POLLIN, duration == -1 ? 0 : duration, () -> !hasClosed.get());
            log.debug("end jdk poll");
            //清除事件
            cqeReadyEventFd.read(cqeReadyMemory.resource(), (int) ValueLayout.JAVA_LONG.byteSize());
            if (log.isDebugEnabled()) {
                log.debug("end poll: {}", cqeReadyMemory.resource().get(ValueLayout.JAVA_LONG, 0));
            }
        }
    }

    @Override
    protected void releaseResource() {
        super.releaseResource();
        Instance.LIBC.close(cqeReadyEventFd.fd());
        cqeReadyMemory.drop();
    }

    static class Poller {
        public static final short POLLIN;
        public static final short POLLOUT;
        public static final short POLLERR;
        public static final short POLLHUP;
        public static final short POLLNVAL;
        public static final short POLLCONN;

        private static final MethodHandle POLLER_POLL_MH;


        static {
            try {
                MethodHandles.Lookup lookup = TrustedLookup.TREUSTED_LOOKUP;
                Class<?> sunNetClazz = Class.forName("sun.nio.ch.Net");
                POLLIN = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLIN", short.class).get();
                POLLOUT = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLOUT", short.class).get();
                POLLERR = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLERR", short.class).get();
                POLLHUP = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLHUP", short.class).get();
                POLLNVAL = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLNVAL", short.class).get();
                POLLCONN = (short)lookup.findStaticVarHandle(sunNetClazz, "POLLCONN", short.class).get();
                Class<?> sunNetPoller = Class.forName("sun.nio.ch.Poller");
                POLLER_POLL_MH = lookup.findStatic(sunNetPoller, "poll", MethodType.methodType(void.class, int.class, int.class, long.class, BooleanSupplier.class));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public static void poll(int fdVal, int event, long nanos, BooleanSupplier isOpenSupplier) {
            try {
                POLLER_POLL_MH.invokeExact(fdVal, event, nanos, isOpenSupplier);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

}
