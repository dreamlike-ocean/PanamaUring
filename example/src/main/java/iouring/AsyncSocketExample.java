package iouring;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.eventloop.IOUringEventLoop;

import java.util.concurrent.ExecutionException;

public class AsyncSocketExample {
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
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            AsyncSocket asyncSocket = eventLoop.openSocket("localhost", port);
            eventLoop.start();
            Integer integer = asyncSocket.connect().get();
            //write示例
            byte[] writeBuf = "hello im io_uring!".getBytes();
            Integer writeRes = asyncSocket.write(writeBuf, 0, writeBuf.length).get();

            //recv示例
//            String s = new String(asyncSocket.recvSelected(1024).get());
            byte[] buf = new byte[1024];
            Integer recvRes = asyncSocket.recv(buf).get();
            String hello = new String(buf, 0, recvRes);
            System.out.println("client recv:" + hello);

            vertx.close();
        } catch (Exception e) {
        }
    }
}
