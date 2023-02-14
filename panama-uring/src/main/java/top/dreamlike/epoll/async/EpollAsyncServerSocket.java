package top.dreamlike.epoll.async;

import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.socket.sockaddr;
import top.dreamlike.nativeLib.socket.socket_h;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLONESHOT;
import static top.dreamlike.nativeLib.inet.inet_h.inet_ntoa;
import static top.dreamlike.nativeLib.inet.inet_h.ntohs;
import static top.dreamlike.nativeLib.string.string_h.strlen;

public class EpollAsyncServerSocket extends AsyncServerSocket {


    public EpollAsyncServerSocket(EpollUringEventLoop eventLoop, String host, int port) {
        super(eventLoop, host, port);
        NativeHelper.makeNoBlocking(serverFd);
    }

    @Override
    public CompletableFuture<EpollAsyncSocket> accept() {
        var res = new CompletableFuture<EpollAsyncSocket>();
        eventLoop.runOnEventLoop(() -> {
            fetchEventLoop().registerEvent(serverFd, EPOLLIN() | EPOLLONESHOT(), (__) -> {
                try (MemorySession memorySession = MemorySession.openConfined()) {
                    MemorySegment client_addr = sockaddr.allocate(memorySession);
                    MemorySegment client_addr_len = memorySession.allocate(JAVA_INT, (int) sockaddr.sizeof());
                    int fd = socket_h.accept(serverFd, client_addr, client_addr_len);
                    if (fd < -1) {
                        res.completeExceptionally(new NativeCallException(NativeHelper.getNowError()));
                        return;
                    }
                    short sin_port = sockaddr_in.sin_port$get(client_addr);
                    int port = Short.toUnsignedInt(ntohs(sin_port));
                    MemoryAddress remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                    long strlen = strlen(remoteHost);
                    String host = new String(MemorySegment.ofAddress(remoteHost, strlen, MemorySession.global()).toArray(JAVA_BYTE));
                    res.complete(new EpollAsyncSocket(fd, host, port, fetchEventLoop()));
                }
            });
        });
        return res;
    }

    @Override
    public EpollUringEventLoop fetchEventLoop() {
        return (EpollUringEventLoop) super.fetchEventLoop();
    }


}
