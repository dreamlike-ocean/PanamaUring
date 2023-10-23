package top.dreamlike.epoll.async;

import io.smallrye.mutiny.Multi;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.socket.extension.AsyncServerSocketOps;
import top.dreamlike.async.socket.extension.EpollFlow;
import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.inet.inet_h;
import top.dreamlike.nativeLib.inet.sockaddr;
import top.dreamlike.nativeLib.inet.sockaddr_in;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLONESHOT;
import static top.dreamlike.nativeLib.inet.inet_h.inet_ntoa;
import static top.dreamlike.nativeLib.inet.inet_h.ntohs;
import static top.dreamlike.nativeLib.string.string_h.strlen;

public final class EpollAsyncServerSocket extends AsyncFd implements AsyncServerSocketOps<EpollAsyncSocket> {
    private final EpollUringEventLoop eventLoop;

    private final int serverFd;


    public EpollAsyncServerSocket(EpollUringEventLoop eventLoop, String host, int port) {
        super(eventLoop);
        this.eventLoop = eventLoop;
        this.serverFd = NativeHelper.serverListen(host, port);
        NativeHelper.makeNoBlocking(serverFd);
    }


    @Override
    public CompletableFuture<EpollAsyncSocket> accept() {
        var res = new CompletableFuture<EpollAsyncSocket>();
        eventLoop.runOnEventLoop(() -> {
            fetchEventLoop().epollMode().registerReadEvent(serverFd, EPOLLIN() | EPOLLONESHOT(), (__) -> {
                try (Arena session = Arena.ofConfined()) {
                    MemorySegment client_addr = sockaddr.allocate(session);
                    MemorySegment client_addr_len = session.allocate(JAVA_INT, (int) sockaddr.sizeof());
                    int fd = inet_h.accept(serverFd, client_addr, client_addr_len);
                    if (fd < -1) {
                        res.completeExceptionally(new NativeCallException(NativeHelper.getNowError()));
                        return;
                    }
                    short sin_port = sockaddr_in.sin_port$get(client_addr);
                    int port = Short.toUnsignedInt(ntohs(sin_port));
                    MemorySegment remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                    long strlen = strlen(remoteHost);
                    String host = new String(remoteHost.reinterpret(strlen).toArray(JAVA_BYTE));
                    res.complete(new EpollAsyncSocket(fd, host, port, fetchEventLoop()));
                }
            });
        });
        return res;
    }

    @Override
    public Flow.Subscription acceptMulti(Consumer<EpollAsyncSocket> socketCallBack) {
        var flow = new EpollFlow<>(Integer.MAX_VALUE, serverFd, eventLoop, socketCallBack);
        eventLoop.runOnEventLoop(() -> {
            fetchEventLoop().epollMode().registerReadEvent(serverFd, EPOLLIN(), (__) -> {
                try (Arena session = Arena.ofConfined()) {
                    MemorySegment client_addr = sockaddr.allocate(session);
                    MemorySegment client_addr_len = session.allocate(JAVA_INT, (int) sockaddr.sizeof());
                    int fd = inet_h.accept(serverFd, client_addr, client_addr_len);
                    if (fd < -1) {
                        flow.onError(new NativeCallException(NativeHelper.getErrorStr(-fd)));
                        flow.cancel();
                        return;
                    }
                    short sin_port = sockaddr_in.sin_port$get(client_addr);
                    int port = Short.toUnsignedInt(ntohs(sin_port));
                    MemorySegment remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                    long strlen = strlen(remoteHost);
                    String host = new String(remoteHost.reinterpret(strlen).toArray(JAVA_BYTE));
                    boolean allowNext = flow.offer(new EpollAsyncSocket(fd, host, port, eventLoop));
                    if (!allowNext) {
                        flow.cancel();
                    }
                }
            });
        });
        return flow;
    }

    public Multi<EpollAsyncSocket> acceptMultiLazy(Supplier<EpollUringEventLoop> epollEventLoopSupplier) {
        return Multi.createFrom()
                .emitter(me -> eventLoop.runOnEventLoop(() -> {
                    fetchEventLoop().epollMode().registerReadEvent(serverFd, EPOLLIN(), (__) -> {

                        if (me.isCancelled()) {
                            fetchEventLoop().epollMode().removeEvent(serverFd, EPOLLIN());
                            return;
                        }

                        try (Arena session = Arena.ofConfined()) {
                            MemorySegment client_addr = sockaddr.allocate(session);
                            MemorySegment client_addr_len = session.allocate(JAVA_INT, (int) sockaddr.sizeof());
                            int fd = inet_h.accept(serverFd, client_addr, client_addr_len);
                            if (fd < -1) {
                                fetchEventLoop().epollMode().removeEvent(serverFd, EPOLLIN());
                                me.fail(new NativeCallException(NativeHelper.getErrorStr(-fd)));
                                return;
                            }
                            short sin_port = sockaddr_in.sin_port$get(client_addr);
                            int port = Short.toUnsignedInt(ntohs(sin_port));
                            MemorySegment remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                            long strlen = strlen(remoteHost);
                            String host = new String(remoteHost.reinterpret(strlen).toArray(JAVA_BYTE));
                            me.emit(new EpollAsyncSocket(fd, host, port, epollEventLoopSupplier.get()));
                        }
                    });
                }));
    }

    public Multi<EpollAsyncSocket> acceptMultiLazy() {
        return acceptMultiLazy(() -> eventLoop);
    }


    @Override
    public EpollUringEventLoop fetchEventLoop() {
        return eventLoop;
    }

    @Override
    public int readFd() {
        throw new UnsupportedOperationException();
    }
}
