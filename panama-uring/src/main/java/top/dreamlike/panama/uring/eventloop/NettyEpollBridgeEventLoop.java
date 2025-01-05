package top.dreamlike.panama.uring.eventloop;

import io.netty.channel.IoEvent;
import io.netty.channel.IoRegistration;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.EpollIoEvent;
import io.netty.channel.epoll.EpollIoHandle;
import io.netty.channel.epoll.EpollIoOps;
import io.netty.channel.unix.FileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.NettyHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

public class NettyEpollBridgeEventLoop extends AbstractNettyBridgeEventLoop implements EpollIoHandle  {

    private static final Logger log = LoggerFactory.getLogger(NettyEpollBridgeEventLoop.class);

    public NettyEpollBridgeEventLoop(SingleThreadIoEventLoop eventLoop, Consumer<IoUringParams> ioUringParamsFactory) {
        super(eventLoop, ioUringParamsFactory);
    }

    protected void setRegistration0(IoRegistration registration) {
        try {
            // 注册io uring的cqe的ready eventfd为读
            registration.submit(EpollIoOps.EPOLLIN);
            log.debug("end setRegistration");
        } catch (Exception e) {
            log.error("register epoll bridge event loop failed", e);
        }
    }

    @Override
    protected EpollIoHandle ioHandle() {
        return this;
    }

    @Override
    public FileDescriptor fd() {
        return nettyFd;
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
        cqeReadyEventFd.read(cqeReadyMemory.resource(), (int) ValueLayout.JAVA_LONG.byteSize());
        //处理cqe
        ioUringCore.processCqes(this::processCqes);
    }

    static {
        if (!NettyHelper.isEpollSupported) {
            throw new IllegalStateException("please import netty-transport-native-epoll");
        }
    }
}
