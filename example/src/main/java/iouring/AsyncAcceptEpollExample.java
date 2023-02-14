package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.epoll.async.EpollAsyncServerSocket;
import top.dreamlike.epoll.async.EpollAsyncSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;

import java.util.concurrent.ExecutionException;

public class AsyncAcceptEpollExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EpollUringEventLoop eventLoop = new EpollUringEventLoop(32, 8, 200);
        Vertx vertx = Vertx.vertx();
        eventLoop.start();
        EpollAsyncServerSocket serverSocket = new EpollAsyncServerSocket(eventLoop, "127.0.0.1", 4399);
        vertx.createNetClient()
                .connect(4399, "127.0.0.1")
                .onSuccess(ns -> {
                    SocketAddress socketAddress = ns.localAddress();
                    System.out.println("vertx local:" + socketAddress.port());
                    System.out.println("vertx remote:" + ns.remoteAddress().port());
                });
        EpollAsyncSocket epollAsyncSocket = serverSocket.accept().get();

        System.out.println(epollAsyncSocket);
    }
}
