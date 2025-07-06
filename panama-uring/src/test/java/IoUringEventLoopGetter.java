import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.uring.IoUringIoHandler;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.eventloop.NettyEpollBridgeEventLoop;
import top.dreamlike.panama.uring.eventloop.NettyIoUringBridgeEventLoop;
import top.dreamlike.panama.uring.eventloop.VTIoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.util.function.Consumer;

public class IoUringEventLoopGetter {

    private static final SingleThreadIoEventLoop EPOLL_EVENT_LOOP;

    private static final SingleThreadIoEventLoop IO_URING_EVENT_LOOP;

    static {
        MultiThreadIoEventLoopGroup epollMainGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        EPOLL_EVENT_LOOP = (SingleThreadIoEventLoop) epollMainGroup.next();

        MultiThreadIoEventLoopGroup ioUringMainGroup = new MultiThreadIoEventLoopGroup(1, IoUringIoHandler.newFactory());
        IO_URING_EVENT_LOOP = (SingleThreadIoEventLoop) ioUringMainGroup.next();
        Runtime.getRuntime().addShutdownHook(new Thread(epollMainGroup::close));
        Runtime.getRuntime().addShutdownHook(new Thread(ioUringMainGroup::close));
    }


    public static IoUringEventLoop get(EventLoopType type,Consumer<IoUringParams> ioUringParamsFactory) {
        return switch (type) {
            case Original -> new IoUringEventLoop(ioUringParamsFactory);
            case VT -> VTIoUringEventLoop.newInstance(ioUringParamsFactory);
            case Netty_Epoll -> new NettyEpollBridgeEventLoop(EPOLL_EVENT_LOOP, ioUringParamsFactory);
            case Netty_IoUring -> new NettyIoUringBridgeEventLoop(IO_URING_EVENT_LOOP, ioUringParamsFactory);
        };
    }

    public enum EventLoopType {
        Original,
        VT,
        Netty_Epoll,
        Netty_IoUring
    }

}
