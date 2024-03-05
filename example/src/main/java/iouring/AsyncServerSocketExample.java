package iouring;

import top.dreamlike.async.AsyncServerSocket;
import top.dreamlike.async.AsyncSocket;
import top.dreamlike.eventloop.IOUringEventLoop;

public class AsyncServerSocketExample {
    public static void main(String[] args) throws Exception {
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            eventLoop.start();
            AsyncServerSocket serverSocket = eventLoop.openServer("127.0.0.1", 8086);
            AsyncSocket asyncSocket = serverSocket.accept().get();
            System.out.println(asyncSocket);
            serverSocket.close();
        }
    }
}
