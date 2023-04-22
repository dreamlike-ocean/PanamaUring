package top.dreamlike.async.socket.extension;

import top.dreamlike.async.socket.AsyncSocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public interface AsyncServerSocketOps<Socket extends AsyncSocket> {

    CompletableFuture<Socket> accept();

    Flow.Subscription acceptMulti(Consumer<Socket> socketCallBack);
}
