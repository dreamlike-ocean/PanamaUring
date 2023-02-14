package top.dreamlike.async.socket;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;

import java.util.concurrent.CompletableFuture;

public non-sealed class AsyncServerSocket extends AsyncFd {

    protected final IOUring uring;
    protected final int serverFd;

    private IOUringEventLoop ioUringEventLoop;

    public AsyncServerSocket(IOUringEventLoop ioUringEventLoop, String host, int port) {
        super(ioUringEventLoop);
        this.uring = AccessHelper.fetchIOURing.apply(ioUringEventLoop);
        this.serverFd = NativeHelper.serverListen(host, port);
    }


    //受限于java泛型限制
    public CompletableFuture<? extends AsyncSocket> accept() {
        CompletableFuture<SocketInfo> res = new CompletableFuture<>();
        ioUringEventLoop.runOnEventLoop(() -> {
            if (!uring.prep_accept(serverFd, res::complete)) {
                res.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return res
                .thenApply(si -> new AsyncSocket(si.fd(), si.host(), si.port(), ioUringEventLoop));
    }

    static {
        AccessHelper.fetchEventLoop = server -> server.ioUringEventLoop;
        AccessHelper.fetchServerFd = server -> server.serverFd;
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return ioUringEventLoop;
    }
}
