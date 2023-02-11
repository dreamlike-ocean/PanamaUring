package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.epoll.async.EpollAsyncSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;

import java.time.LocalDateTime;
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

                    vertx.setPeriodic(1000, l -> {
                        socket.write("hello io_uring!" + LocalDateTime.now());
                    });
                })
                .listen(port).toCompletionStage().toCompletableFuture().get();

        EpollUringEventLoop eventLoop = new EpollUringEventLoop(16, 8, 2000);
        eventLoop.start();
        EpollAsyncSocket socket = eventLoop.openSocket("localhost", 1234);
        Integer integer = socket.connect().get();
        System.out.println("connect");
        while (true) {
            byte[] bytes = new byte[1024];
            Integer recvLength = socket.recv(bytes)
                    .get();
            System.out.println(new String(bytes, 0, recvLength));
        }
    }
}
