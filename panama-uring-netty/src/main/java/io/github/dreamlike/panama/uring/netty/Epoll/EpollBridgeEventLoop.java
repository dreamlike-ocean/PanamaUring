package io.github.dreamlike.panama.uring.netty.Epoll;

import io.github.dreamlike.panama.uring.netty.NettyHelper;
import io.netty.channel.IoEvent;
import io.netty.channel.IoRegistration;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.EpollIoEvent;
import io.netty.channel.epoll.EpollIoHandle;
import io.netty.channel.epoll.EpollIoOps;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.eventloop.BridgeEventLoop;
import top.dreamlike.panama.uring.helper.JemallocAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EpollBridgeEventLoop extends BridgeEventLoop implements EpollIoHandle {

    private static final Logger log = LoggerFactory.getLogger(EpollBridgeEventLoop.class);

    private final SingleThreadIoEventLoop eventLoop;

    private final EventFd cqeReadyEventFd;
    private final MemorySegment cqeReadyMemory;
    private final FileDescriptor fd;
    private IoRegistration registration;

    /**
     * 只在eventloop上跑所以不需要volatile
     */
    private boolean haveSqe = false;

    public EpollBridgeEventLoop(SingleThreadIoEventLoop eventLoop, Consumer<IoUringParams> ioUringParamsFactory) {
        super(NettyHelper.getEventLoopThread(eventLoop), ioUringParamsFactory);
        this.eventLoop = eventLoop;
        this.cqeReadyEventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);

        int sysCall = libUring.io_uring_register_eventfd(internalRing, cqeReadyEventFd.fd());
        if (sysCall < 0) {
            throw new SyscallException(sysCall);
        }

        this.cqeReadyMemory = JemallocAllocator.INSTANCE.allocate(ValueLayout.JAVA_LONG);

        this.fd = new FileDescriptor(cqeReadyEventFd.fd());
        Future<IoRegistration> future = eventLoop.register(this);
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
        try {
            // 注册io uring的cqe的ready eventfd为读
            registration.submit(EpollIoOps.EPOLLIN);
            log.debug("end setRegistration");
        } catch (Exception e) {
            log.error("register epoll bridge event loop failed", e);
        }
    }

    @Override
    public FileDescriptor fd() {
        return fd;
    }

    @Override
    public void handle(IoRegistration ioRegistration, IoEvent ioEvent) {
        log.debug("Netty eventLoop find cqe eventFd is readable");
        EpollIoEvent epollIoEvent = (EpollIoEvent) ioEvent;
        boolean needHandleCqes = epollIoEvent.ops().contains(EpollIoOps.EPOLLIN);
        if (!needHandleCqes) {
            log.debug("skip handle cqe");
            return;
        }

        //清除事件
        cqeReadyEventFd.read(cqeReadyMemory, (int) ValueLayout.JAVA_LONG.byteSize());
        //处理cqe
        processCqes();
    }

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
            libUring.io_uring_submit(internalRing);
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
       });
    }

    @Override
    public boolean inEventLoop() {
        return eventLoop.inEventLoop();
    }

    static {
        if (!NettyHelper.isEpollSupported) {
            throw new IllegalStateException("please import netty-transport-native-epoll");
        }
    }
}
