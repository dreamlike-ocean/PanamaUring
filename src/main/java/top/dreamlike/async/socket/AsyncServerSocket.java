package top.dreamlike.async.socket;

import top.dreamlike.async.IOUring;
import top.dreamlike.helper.SocketHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.nativeLib.socket.socket_h.accept;

public class AsyncServerSocket {

    private final IOUring uring;
    private final int serverFd;

    public AsyncServerSocket(IOUring uring,String host,int port){
        this.uring = uring;
        this.serverFd = SocketHelper.serverListen(host, port);
    }


    public CompletableFuture<AsyncSocket> acceptAsync(){
        CompletableFuture<AsyncSocket> res = new CompletableFuture<>();
        uring.prep_accept(serverFd, res::complete);
        uring.submit();
        return res;
    }
}
