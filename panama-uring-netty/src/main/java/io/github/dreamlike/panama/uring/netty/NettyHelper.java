package io.github.dreamlike.panama.uring.netty;

import io.netty.util.concurrent.SingleThreadEventExecutor;
import top.dreamlike.unsafe.core.MasterKey;

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

        MethodHandles.Lookup lookup = MasterKey.INSTANCE.getTrustedLookup();
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
