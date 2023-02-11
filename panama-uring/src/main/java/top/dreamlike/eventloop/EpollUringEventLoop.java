package top.dreamlike.eventloop;


import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.epoll.async.EpollAsyncSocket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;

public class EpollUringEventLoop extends IOUringEventLoop {
    private final EpollEventLoop epollEventLoop;

    private final int ioUringEventFd;


    public EpollUringEventLoop(int ringSize, int autoBufferSize, long autoSubmitDuration) {
        //todo 异常情况关闭io_uring and epoll,先假定不会出问题 java也没法支持捕获native错误
        super(ringSize, autoBufferSize, autoSubmitDuration);
        epollEventLoop = new EpollEventLoop();
        epollEventLoop.tasks = tasks;
        ioUringEventFd = ioUring.registerEventFd();
        epollEventLoop.registerEvent(ioUringEventFd, EPOLLIN(), (__) -> {
            //收割cqe
            super.afterSelect();
        });
    }


    @Override
    public AsyncSocket openSocket(String host, int port) {
        return new EpollAsyncSocket(new InetSocketAddress(host, port), this);
    }

    public CompletableFuture<Void> registerEvent(int fd, int event, IntConsumer callback) {
        return epollEventLoop.registerEvent(fd, event, callback);
    }

    public CompletableFuture<Void> modifyCallBack(int fd, IntConsumer callback) {
        return epollEventLoop.modifyCallBack(fd, callback);
    }

    public CompletableFuture<Void> modifyEvent(int fd, int event) {
        return epollEventLoop.modifyEvent(fd, event);
    }

    public CompletableFuture<Void> unregisterEvent(int fd) {
        return epollEventLoop.unregisterEvent(fd);
    }

    @Override
    protected void close0() throws Exception {
        super.close0();
        epollEventLoop.close0();
    }

    @Override
    protected void afterSelect() {
        epollEventLoop.afterSelect();
    }


    @Override
    public void wakeup() {
        //启动时没有
        if (epollEventLoop != null) {
            epollEventLoop.wakeup();
        }
    }
}
