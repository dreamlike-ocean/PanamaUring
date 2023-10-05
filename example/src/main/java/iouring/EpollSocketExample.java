package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.epoll.async.EpollAsyncSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;

import java.util.concurrent.ExecutionException;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_RDONLY;

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
                        socket.write("hello io_uring!");
                    });
                })
                .listen(port).toCompletionStage().toCompletableFuture().get();

        EpollUringEventLoop eventLoop = new EpollUringEventLoop(16, 8, 2000);
        eventLoop.start();
        EpollAsyncSocket socket = eventLoop.openSocket("localhost", 1234);
        //socket connect 基于epoll做的 这里简单直接get阻塞到完成
        Integer integer = socket.connect().get();
        System.out.println("connect");

        //文件异步io 基于io_uring做的
        AsyncFile asyncFile = eventLoop.openFile("demo.txt", O_RDONLY());
        asyncFile.read(0, 1024)
                .thenAccept(b -> System.out.println("file read" + new String(b)));


        while (true) {
            byte[] bytes = new byte[1024];
            //socket read 基于epoll做的 这里简单直接get阻塞到完成
            Integer recvLength = socket.recv(bytes)
                    .get().intValue();
            System.out.println(new String(bytes, 0, recvLength));
        }
    }
}
