package top.dreamlike.panama.uring.helper;

import io.netty.util.concurrent.SingleThreadEventExecutor;
import top.dreamlike.panama.uring.helper.unsafe.TrustedLookup;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class NettyHelper {

    public static final boolean isEpollSupported;

    public static final boolean isIoUringSupported;

    private static final VarHandle EVENTLOO_THREAD;

    static {
        boolean haveEpoll;
        try {
            Class.forName("io.netty.channel.epoll.EpollIoHandler");
            haveEpoll = true;
        } catch (ClassNotFoundException e) {
            haveEpoll = false;
        }

        isEpollSupported = haveEpoll;

        boolean haveIoUring;
        try {
            Class.forName("io.netty.channel.uring.IoUringIoHandler");
            haveIoUring = true;
        } catch (ClassNotFoundException e) {
            haveIoUring = false;
        }

        isIoUringSupported = haveIoUring;

        MethodHandles.Lookup lookup = TrustedLookup.TREUSTED_LOOKUP;
        try {
            EVENTLOO_THREAD = lookup.findVarHandle(SingleThreadEventExecutor.class, "thread", Thread.class);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Thread getEventLoopThread(SingleThreadEventExecutor eventLoop) {
        return (Thread) EVENTLOO_THREAD.get(eventLoop);
    }
}
