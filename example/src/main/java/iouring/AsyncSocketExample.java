package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;

import java.util.concurrent.ExecutionException;

public class AsyncSocketExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Vertx vertx = Vertx.vertx();
        int port = 1234;
        vertx.createNetServer()
                .connectHandler(socket -> {
                    SocketAddress x = socket.remoteAddress();
                    System.out.println(x.hostAddress() + "port:" + x.port());
                })
                .listen(port).toCompletionStage().toCompletableFuture().get();
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            AsyncSocket asyncSocket = eventLoop.openSocket("localhost", port);
            eventLoop.start();
            Integer integer = asyncSocket.connect().get();
            if (integer < 0) {
                System.out.println(NativeHelper.getErrorStr(-integer));
            }

//            vertx.close();
        } catch (Exception e) {
        }
    }
}
