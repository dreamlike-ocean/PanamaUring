package top.dreamlike.async;

import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.epoll.async.EpollAsyncServerSocket;
import top.dreamlike.eventloop.IOUringEventLoop;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

sealed public abstract class AsyncFd implements EventLoopAccess permits PlainAsyncFd, AsyncWatchService, AsyncServerSocket, AsyncSocket, EpollAsyncServerSocket {

    protected IOUringEventLoop eventLoop;

    private static final Consumer<Integer> DO_NOTHING = i -> {
    };

    protected AsyncFd(IOUringEventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    protected void onTermination(AtomicBoolean end, long userData) {
        //cas失败 当前异步操作已经完成 无需cancel
        if (!end.compareAndSet(false, true)) {
            return;
        }
        // 非正常结束比如说cancel了
        eventLoop.cancelAsync(userData, 0, true)
                //一并收拢到io_uring回调中释放资源
                .subscribe().with(DO_NOTHING, t -> {
                    // todo 这里异常被吞了。。。
                    // 取消失败 不释放资源等待async op回调
                });
    }
}
