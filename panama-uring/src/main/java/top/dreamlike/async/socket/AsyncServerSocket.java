package top.dreamlike.async.socket;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;

import java.util.concurrent.CompletableFuture;

public class AsyncServerSocket {

    private final IOUring uring;
    private final int serverFd;

    private final IOUringEventLoop ioUringEventLoop;

    public AsyncServerSocket(IOUringEventLoop ioUringEventLoop,String host,int port){
        this.uring = AccessHelper.fetchIOURing.apply(ioUringEventLoop);
        this.ioUringEventLoop = ioUringEventLoop;
        this.serverFd = NativeHelper.serverListen(host, port);
    }


    public CompletableFuture<AsyncSocket> accept(){
        CompletableFuture<SocketInfo> res = new CompletableFuture<>();
        ioUringEventLoop.runOnEventLoop(() -> {
            if (!uring.prep_accept(serverFd, res::complete)) {
                res.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return res
                .thenApply(si -> new AsyncSocket(si.fd(),si.host(),si.port(),ioUringEventLoop));
    }

    static {
        AccessHelper.fetchEventLoop = server -> server.ioUringEventLoop;
        AccessHelper.fetchServerFd = server -> server.serverFd;
    }
}
