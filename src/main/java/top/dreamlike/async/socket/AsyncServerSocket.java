package top.dreamlike.async.socket;

import top.dreamlike.async.uring.IOUring;
import top.dreamlike.helper.SocketHelper;

import java.util.concurrent.CompletableFuture;

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
        return res;
    }
}
