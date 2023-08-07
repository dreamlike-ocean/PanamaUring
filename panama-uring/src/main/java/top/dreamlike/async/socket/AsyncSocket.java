package top.dreamlike.async.socket;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.socket.extension.IOUringFlow;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.NotEnoughSqeException;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public non-sealed class AsyncSocket extends AsyncFd {
    // todo 区分本地地址和远端地址
    // 目前是靠对于用途对host和port解释不同
    protected final int fd;
    protected final String host;
    protected final int port;

    private final IOUring ring;

    protected final IOUringEventLoop eventLoop;

    public AsyncSocket(int fd, String host, int port, IOUringEventLoop eventLoop) {
        super(eventLoop);
        this.fd = fd;
        this.host = host;
        this.port = port;
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
    }

    public AsyncSocket(SocketAddress address, IOUringEventLoop eventLoop) {
        super(eventLoop);
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
        switch (address) {
            case InetSocketAddress remote -> {
                host = remote.getHostString();
                port = remote.getPort();
                fd = remote.getAddress() instanceof Inet6Address ? NativeHelper.tcpClientSocketV6()
                        : NativeHelper.tcpClientSocket();
            }
            case UnixDomainSocketAddress local -> {
                // todo uds有空再做
                throw new IllegalStateException("uds unsupported");
            }
            default -> throw new IllegalStateException("Unexpected value: " + address);
        }
    }

    @Deprecated
    public CompletableFuture<byte[]> recvSelected(int size) {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ring.prep_selected_recv(fd, size, completableFuture)) {
                completableFuture.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return completableFuture;
    }

    @Deprecated
    public Flow.Subscription recvMulti(Consumer<byte[]> consumer) {
        IOUringFlow<byte[]> flow = new IOUringFlow<>(Integer.MAX_VALUE, eventLoop, consumer);
        eventLoop.runOnEventLoop(() -> {
            long userData = ring.prep_recv_multi_and_get_user_data(fd, (res, throwable) -> {
                if (throwable != null) {
                    flow.onError(throwable);
                    flow.cancel();
                    return;
                }
                boolean allowNext = flow.offer(res);
                if (!allowNext) {
                    flow.cancel();
                }
            });
            flow.setUserData(userData);
        });
        return flow;
    }

    public Multi<byte[]> recvMulti() {
        return Multi.createFrom()
                .emitter(me -> {
                    eventLoop.runOnEventLoop(() -> {
                        // 这个Aomtic只是为了方便给lambda传递userData而已
                        AtomicLong userDataLazy = new AtomicLong();
                        //挂载回调
                        long userData = ring.prep_recv_multi_and_get_user_data(fd, (res, throwable) -> {
                            if (throwable != null) {
                                me.fail(throwable);
                                eventLoop.cancelAsync(userDataLazy.get(), 0, true);
                                return;
                            }
                            //这里cancel是有意义的因为是multi-shot的让下次不再发射事件 res直接释放就好了
                            if (me.isCancelled()) {

                                eventLoop.cancelAsync(userDataLazy.get(), 0, true);
                                return;
                            }
                            me.emit(res);
                            // 考虑是否为0的情况下终止这个流
                            if (res.length == 0) {
                                me.complete();
                            }

                        });
                    
                        if (userData != IOUring.NO_SQE) {
                            userDataLazy.set(userData);
                            return;
                        }

                        //如果sqe不足 直接发射失败事件
                        me.fail(new NotEnoughSqeException());
                        me.complete();
                    });
                });
    }

    public CompletableFuture<Integer> recv(byte[] buffer) {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        Arena malloc = Arena.openShared();
        MemorySegment buf = malloc.allocateArray(JAVA_BYTE, buffer.length);
        eventLoop.runOnEventLoop(() -> {
            boolean res = ring.prep_recv(fd, buf, completableFuture::complete);
            if (!res) {
                completableFuture.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return completableFuture
                .thenCompose(res -> res < 0
                        ? CompletableFuture.failedFuture(new NativeCallException(NativeHelper.getErrorStr(-res)))
                        : CompletableFuture.completedFuture(res))
                .thenApply(i -> {
                    MemorySegment.copy(buf, 0, MemorySegment.ofArray(buffer), 0, i);
                    return i;
                })
                .whenComplete((__, ___) -> malloc.close());
    }

    public CompletableFuture<Integer> connect() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            try {
                if (!ring.prep_connect(new SocketInfo(fd, host, port), future::complete)) {
                    future.completeExceptionally(new NotEnoughSqeException());
                }
            } catch (UnknownHostException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Integer> write(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Arena session = Arena.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(length);
        MemorySegment.copy(buffer, offset, memorySegment, JAVA_BYTE, 0, length);
        eventLoop.runOnEventLoop(() -> {
            if (!ring.prep_send(fd, memorySegment, future::complete)) {
                future.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return future.whenComplete((res, t) -> {
            session.close();
        });
    }

    public CompletableFuture<Integer> write(byte[] buffer) {
        return write(buffer, 0, buffer.length);
    }

    @Override
    public String toString() {
        return "AsyncSocket{" +
                "res=" + fd +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", ring=" + ring +
                '}';
    }

    public SocketAddress getAddress() {
        return InetSocketAddress.createUnresolved(host, port);
    }

    public InetSocketAddress getInetAddress() {
        return InetSocketAddress.createUnresolved(host, port);
    }

    static {
        AccessHelper.fetchSocketFd = a -> a.fd;
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }
}
