package top.dreamlike.async;

import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

sealed public abstract class AsyncFd implements EventLoopAccess permits PlainAsyncFd, AsyncWatchService, AsyncServerSocket, AsyncSocket, EpollAsyncServerSocket {

    protected IOUringEventLoop eventLoop;

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected static final Consumer<Integer> DO_NOTHING = i -> {
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

    public boolean closed() {
        return closed.get();
    }

    public abstract int readFd();

    public void close() {
        eventLoop.runOnEventLoop(() -> {
            if (closed.compareAndSet(false, true)) {
                try {
                    if (readFd() != writeFd()) {
                        unistd_h.close(writeFd());
                    }
                    unistd_h.close(readFd());
                } catch (RuntimeException e) {
                    closed.set(false);
                    throw e;
                }
            }
        });
    }

    public int writeFd() {
        return readFd();
    }
}
