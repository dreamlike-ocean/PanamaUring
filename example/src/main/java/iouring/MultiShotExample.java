package iouring;


import io.vertx.core.Vertx;
import top.dreamlike.async.AsyncServerSocket;
import top.dreamlike.eventloop.IOUringEventLoop;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiShotExample {
    private static Vertx vertx;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        vertx = Vertx.vertx();
        IOUringEventLoop loop = new IOUringEventLoop(32, 8, 100);
        loop.start();
        AsyncServerSocket serverSocket = loop.openServer("127.0.0.1", 4399);
        var count = new AtomicInteger();
        createTcpConn(4399);
        count.incrementAndGet();
        var resFlow = serverSocket.acceptMulti(as -> {
            var address = as.getInetAddress();
//            System.out.println("accept:" + address.getPort() +"[]");
            System.out.printf("accept: %s,Thread : %s\n", address.getPort(), Thread.currentThread());
            if (count.getAndIncrement() <= 10) {
                createTcpConn(4399);
            }
        });


        resFlow.request(10);

        Thread.sleep(10000);
        resFlow.cancel();

        System.out.println(count);
    }

    public static void createTcpConn(int port) {
        vertx.createNetClient()
                .connect(port, "127.0.0.1")
                .onSuccess(ns -> {
                    System.out.println("connect! local port:" + ns.localAddress().port());
                })
                .onFailure(Throwable::printStackTrace);

    }

}