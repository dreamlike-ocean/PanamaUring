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
import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class VTIoUringEventLoop extends IoUringEventLoop {

    private static final Logger log = LoggerFactory.getLogger(VTIoUringEventLoop.class);
    private final EventFd cqeReadyEventFd;
    private final OwnershipMemory cqeReadyMemory;
    private final Executor vtScheduler;
    private final Thread carrierThread;

    private VTIoUringEventLoop(Consumer<IoUringParams> ioUringParamsFactory, ThreadFactory factory, Thread carrierThread, Executor vtScheduler) {
        super(ioUringParamsFactory, factory);
        this.vtScheduler = vtScheduler;
        this.carrierThread = carrierThread;
        //poll的时候不一定有事件 也可能是单纯的超时了
        cqeReadyEventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);
        int sysCall = libUring.io_uring_register_eventfd(internalRing, cqeReadyEventFd.fd());
        if (sysCall < 0) {
            throw new SyscallException(sysCall);
        }
        cqeReadyMemory = memoryAllocator.allocateOwnerShipMemory(ValueLayout.JAVA_LONG.byteSize());
    }

    public static VTIoUringEventLoop newInstance(Consumer<IoUringParams> ioUringParamsFactory) {
        return newInstance("VT-IoUring-CarrierThread", ioUringParamsFactory);
    }

    public static VTIoUringEventLoop newInstance(String threadName, Consumer<IoUringParams> ioUringParamsFactory) {
        // 受限于super的语法限制所以这里用factory模式构建
        LinkedBlockingQueue<Runnable> continuationQueue = new LinkedBlockingQueue<>();
        VTIoUringEventLoop[] lazyBox = new VTIoUringEventLoop[1];
        Thread carrierThread = new Thread(() -> {
            Thread.currentThread().setName(threadName);
            while (true) {
                boolean close = lazyBox[0] != null && lazyBox[0].closed();
                if (close) {
                    break;
                }
                try {
                    continuationQueue.take().run();
                } catch (Throwable e) {
                    log.error("run continuation error!", e);
                }
            }
            continuationQueue.forEach(Runnable::run);
        });
        Executor vtScheduler = continuationQueue::offer;
        ThreadFactory ioVTFactory = LoomSupport.setScheduler(Thread.ofVirtual().name("IoUringEventLoop-VT-" + count.incrementAndGet()), vtScheduler)
                .factory();
        VTIoUringEventLoop vtIoUringEventLoop = new VTIoUringEventLoop(ioUringParamsFactory, ioVTFactory, carrierThread, vtScheduler);
        lazyBox[0] = vtIoUringEventLoop;
        return vtIoUringEventLoop;
    }

    public ThreadFactory asVTScheduler() {
        return LoomSupport.setScheduler(Thread.ofVirtual(), vtScheduler)
                .factory();
    }

    @Override
    public void start() {
        carrierThread.start();
        super.start();
    }

    @Override
    protected void submitAndWait(long duration) {
        int cqeCount = libUring.io_uring_submit_and_wait(internalRing, 0);
        log.debug("cqeCount: {}, duration: {}", cqeCount, duration);
        assert Thread.currentThread().isVirtual();
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
                POLLIN = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLIN", short.class).get();
                POLLOUT = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLOUT", short.class).get();
                POLLERR = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLERR", short.class).get();
                POLLHUP = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLHUP", short.class).get();
                POLLNVAL = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLNVAL", short.class).get();
                POLLCONN = (short) lookup.findStaticVarHandle(sunNetClazz, "POLLCONN", short.class).get();
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

    public static class LoomSupport {
        private static final MethodHandle SET_SCHEDULER_MH;
        private static final MethodHandle CURRENT_CARRIER_THREAD_MH;

        static {
            MethodHandles.Lookup lookup = TrustedLookup.currentSupportedLookup();
            MethodHandle setSchedulerMH;
            try {
                Class<? extends Thread.Builder.OfVirtual> aClass = Thread.ofVirtual().getClass();
                Field scheduler = aClass.getDeclaredField("scheduler");
                setSchedulerMH = lookup.unreflectSetter(scheduler);
            } catch (Throwable e) {
                setSchedulerMH = null;
            }
            SET_SCHEDULER_MH = setSchedulerMH;

            MethodHandle currentCarrierMH;
            try {
                MethodHandle currentCarrierThread = lookup.findStatic(Thread.class, "currentCarrierThread", MethodType.methodType(Thread.class));
                currentCarrierMH = currentCarrierThread;
            } catch (Throwable e) {
                currentCarrierMH = null;
            }
            CURRENT_CARRIER_THREAD_MH = currentCarrierMH;
        }

        private static void assertSupported() {
            if (SET_SCHEDULER_MH == null) {
                throw new UnsupportedOperationException("vt not supported");
            }
        }

        public static Thread carrierThread() {
            if (CURRENT_CARRIER_THREAD_MH == null) {
                throw new UnsupportedOperationException("carrierThread not supported");
            }
            try {
                return (Thread) CURRENT_CARRIER_THREAD_MH.invokeExact();
            } catch (Throwable e) {
                // should not happen
                throw new RuntimeException(e);
            }
        }

        private static Thread.Builder.OfVirtual setScheduler(Thread.Builder.OfVirtual builder, Executor executor) {
            assertSupported();
            try {
                SET_SCHEDULER_MH.invoke(builder, executor);
                return builder;
            } catch (Throwable e) {
                // should not happen
                throw new RuntimeException(e);
            }
        }
    }
}
