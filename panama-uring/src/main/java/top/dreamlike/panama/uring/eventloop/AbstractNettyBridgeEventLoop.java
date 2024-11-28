package top.dreamlike.panama.uring.eventloop;

import io.netty.channel.IoHandle;
import io.netty.channel.IoRegistration;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.JemallocAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public non-sealed abstract class AbstractNettyBridgeEventLoop extends IoUringEventLoop {

    private static final Logger log = LoggerFactory.getLogger(AbstractNettyBridgeEventLoop.class);

    private final SingleThreadIoEventLoop eventLoop;

    protected final EventFd cqeReadyEventFd;
    protected final MemorySegment cqeReadyMemory;
    protected final FileDescriptor nettyFd;
    protected IoRegistration registration;
    protected final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * 只在eventloop上跑所以不需要volatile
     */
    private boolean haveSqe = false;

    public AbstractNettyBridgeEventLoop(SingleThreadIoEventLoop eventLoop, Consumer<IoUringParams> ioUringParamsFactory) {
        super(ioUringParamsFactory, (_) -> null);
        this.eventLoop = eventLoop;
        this.cqeReadyEventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);

        int sysCall = libUring.io_uring_register_eventfd(internalRing, cqeReadyEventFd.fd());
        if (sysCall < 0) {
            throw new SyscallException(sysCall);
        }

        this.cqeReadyMemory = JemallocAllocator.INSTANCE.allocate(ValueLayout.JAVA_LONG);
        this.nettyFd = new FileDescriptor(cqeReadyEventFd.fd());

        Future<IoRegistration> future = eventLoop.register(ioHandle());
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                setRegistration((IoRegistration) future1.resultNow());
            } else {
                log.error("register epoll bridge event loop failed", future1.cause());
            }
        });

        eventLoop.addShutdownHook(() -> {
            try {
                close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setRegistration(IoRegistration registration) {
        this.registration = registration;
        setRegistration0(registration);
    }

    protected abstract void setRegistration0(IoRegistration registration);

    protected abstract IoHandle ioHandle();

    @Override
    public void execute(Runnable command) {
        eventLoop.execute(command);
    }

    @Override
    public void runOnEventLoop(Runnable runnable) {
        if (eventLoop.inEventLoop()) {
            runnable.run();
        } else {
            eventLoop.execute(runnable);
        }
    }

    @Override
    protected void afterFill(IoUringSqe nativeSqe) {
        if (haveSqe) {
            return;
        }
        haveSqe = true;
        eventLoop.executeAfterEventLoopIteration(this::flush);
    }

    @Override
    public void flush() {
        if (inEventLoop()) {
            ioUringCore.submit();
            haveSqe = false;
        } else {
            execute(this::flush);
        }
    }

    @Override
    public void submitScheduleTask(long delay, TimeUnit timeUnit, Runnable task) {
        eventLoop.schedule(task, delay, timeUnit);
    }

    @Override
    protected void releaseResource() {
       runOnEventLoop(() -> {
           super.releaseResource();
           if(!eventLoop.isShutdown()) {
               try {
                   registration.cancel();
               } catch (Exception e) {
                  log.error("cancel error!", e);
               }
           }
           Instance.LIBC.close(cqeReadyEventFd.fd());
           Instance.LIB_JEMALLOC.free(cqeReadyMemory);
           shutdownLatch.countDown();
       });
    }

    @Override
    public boolean inEventLoop() {
        return eventLoop.inEventLoop();
    }

    @Override
    protected boolean needWakeUpFd() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public void start() {
        //do nothing
    }

    @Override
    public void run() {

    }

    @Override
    public void close() throws Exception {
        if (hasClosed.compareAndSet(false, true)) {
            runOnEventLoop(this::releaseResource);
        }
    }

    @Override
    public void join() throws InterruptedException {
        shutdownLatch.await();
    }
}
