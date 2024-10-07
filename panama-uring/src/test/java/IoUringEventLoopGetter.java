import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.EpollIoHandler;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.eventloop.NettyEpollBridgeEventLoop;
import top.dreamlike.panama.uring.eventloop.VTIoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.util.function.Consumer;

public class IoUringEventLoopGetter {

    private static final SingleThreadIoEventLoop EVENT_LOOP;

    static {
        MultiThreadIoEventLoopGroup mainGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        EVENT_LOOP = (SingleThreadIoEventLoop) mainGroup.next();
        Runtime.getRuntime().addShutdownHook(new Thread(mainGroup::close));
    }


    public static IoUringEventLoop get(EventLoopType type,Consumer<IoUringParams> ioUringParamsFactory) {
        return switch (type) {
            case Original -> new IoUringEventLoop(ioUringParamsFactory);
            case VT -> new VTIoUringEventLoop(ioUringParamsFactory);
            case Netty_Epoll -> new NettyEpollBridgeEventLoop(EVENT_LOOP, ioUringParamsFactory);
        };
    }

    public enum EventLoopType {
        Original,
        VT,
        Netty_Epoll
    }

}
