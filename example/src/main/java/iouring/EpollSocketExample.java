package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;

import java.util.concurrent.ExecutionException;

public class EpollSocketExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        Vertx vertx = Vertx.vertx();
        int port = 1234;
        vertx.createNetServer()
                .connectHandler(socket -> {
                    SocketAddress x = socket.remoteAddress();
                    socket.handler(b -> System.out.println("server recv:" + b));
                    System.out.println("remote connect!:" + x.hostAddress() + "port:" + x.port());
                    socket.write("hello io_uring!");
                })
                .listen(port).toCompletionStage().toCompletableFuture().get();

        EpollUringEventLoop eventLoop = new EpollUringEventLoop(16, 8, 2000);
        eventLoop.start();
        AsyncSocket socket = eventLoop.openSocket("localhost", 1234);
        System.out.println(socket.connect().get());
    }
}
