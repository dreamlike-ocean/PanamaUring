package top.dreamlike.async.socket;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.NotEnoughSqeException;
import top.dreamlike.extension.fp.Result;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public non-sealed class AsyncSocket extends AsyncFd {
    // todo 区分本地地址和远端地址
    // 目前是靠对于用途对host和port解释不同
    protected final int fd;
    protected final String host;
    protected final int port;

    private final IOUring ring;

    protected final IOUringEventLoop eventLoop;

    protected static int INIT = 0;
    protected static int CONNECTING = 1;
    protected static int CONNECTED = 2;
    protected static int CLOSE = 3;
    protected AtomicInteger state;

    protected AtomicBoolean multiMode = new AtomicBoolean(false);

    /**
     * accept 使用用到
     *
     * @param fd        socket fd
     * @param host      对端host
     * @param port      对端端口
     * @param eventLoop 绑定的eventloop
     */
    public AsyncSocket(int fd, String host, int port, IOUringEventLoop eventLoop) {
        super(eventLoop);
        this.fd = fd;
        this.host = host;
        this.port = port;
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
        this.state = new AtomicInteger(CONNECTED);
    }

    public AsyncSocket(SocketAddress address, IOUringEventLoop eventLoop) {
        super(eventLoop);
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
        this.state = new AtomicInteger(INIT);
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
        checkStateBeforeSingleRecv();

        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ring.prep_selected_recv(fd, size, completableFuture)) {
                completableFuture.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return completableFuture;
    }


    public Uni<byte[]> recvSelectLazy(int size) {
        return recvSelectLazy(size, true);
    }

    public Uni<byte[]> recvSelectLazy(int size, boolean closeWhenEOF) {
        checkStateBeforeSingleRecv();
        return Uni.createFrom()
                .emitter(ue -> eventLoop.runOnEventLoop(() -> {
                    AtomicBoolean end = new AtomicBoolean(false);
                    long userData = ring.prep_selected_recv_and_get_user_data(fd, size, (r) -> {
                        if (!end.compareAndSet(false, true)) {
                            return;
                        }
                        switch (r) {
                            case Result.Err(Throwable t) -> ue.fail(t);
                            case Result.OK(byte[] res) -> {
                                if (res.length == 0 && closeWhenEOF) {
                                    if (state.compareAndSet(CONNECTED, CLOSE)) {
                                        unistd_h.close(fd);
                                    }
                                    ue.fail(new IllegalStateException("socket has closed"));
                                } else {
                                    ue.complete(res);
                                }
                            }
                        }
                    });
                    if (userData == IOUring.NO_SQE) {
                        end.set(true);
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }
                    ue.onTermination(() -> onTermination(end, userData));
                }));
    }

    /**
     * 不要在此之外直接cancel这个async op
     *
     * @return
     */
    public Multi<byte[]> recvMulti() {
        if (!multiMode.compareAndSet(false, true)) {
            throw new IllegalStateException("current state is in multiMode");
        }
        return Multi.createFrom()
                .emitter(me -> {
                    eventLoop.runOnEventLoop(() -> {
                        if (state.get() != CONNECTED) {
                            me.fail(new IllegalStateException("Socket is not connected"));
                            return;
                        }
                        // 这个Atomic只是为了方便给lambda传递userData而已
                        AtomicLong userDataLazy = new AtomicLong();
                        AtomicBoolean hasCancel = new AtomicBoolean(false);
                        //挂载回调
                        long userData = ring.prep_recv_multi_and_get_user_data(fd, (res, throwable) -> {
                            if (throwable != null && state.get() != CLOSE) {
                                me.fail(throwable);
                                return;
                            }
                            me.emit(res);
                            // 考虑是否为0的情况下终止这个流
                            if (res.length == 0) {
                                if (state.compareAndSet(CONNECTED, CLOSE)) {
                                    unistd_h.close(fd);
                                }
                                me.fail(new SocketException("socket has been closed"));
                            }
                        });


                        if (userData != IOUring.NO_SQE) {
                            userDataLazy.set(userData);
                            me.onTermination(() -> onTermination(hasCancel, userData));
                            return;
                        }

                        //如果sqe不足 直接发射失败事件
                        me.fail(new NotEnoughSqeException());
                        hasCancel.setRelease(true);
                        me.complete();
                    });
                });
    }

    @Deprecated
    public CompletableFuture<Long> recv(byte[] buffer) {
        checkStateBeforeSingleRecv();
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();
        Arena malloc = Arena.ofShared();
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
                    return Long.valueOf(i);
                })
                .whenComplete((__, ___) -> malloc.close());
    }

    public Uni<Integer> recvLazy(byte[] buffer) {
        return recvLazy(buffer, true);
    }

    public Uni<Integer> recvLazy(byte[] buffer, boolean closeWhenEOF) {
        checkStateBeforeSingleRecv();
        return Uni.createFrom()
                .emitter(ue -> eventLoop.runOnEventLoop(() -> {
                    Arena malloc = Arena.ofShared();
                    MemorySegment buf = malloc.allocateArray(JAVA_BYTE, buffer.length);
                    AtomicBoolean end = new AtomicBoolean(false);
                    long userData = ring.prep_recv_and_get_user_data(fd, buf, i -> {
                        if (!end.compareAndSet(false, true)) {
                            return;
                        }
                        if (i < 0) {
                            ue.fail(new NativeCallException(NativeHelper.getErrorStr(-i)));
                        } else {
                            if (i == 0 && closeWhenEOF) {
                                if (state.compareAndSet(CONNECTED, CLOSE)) {
                                    unistd_h.close(fd);
                                }
                                ue.fail(new IllegalStateException("socket has closed"));
                                return;
                            } else {
                                ue.complete(i);
                            }
                            MemorySegment.copy(buf, 0, MemorySegment.ofArray(buffer), 0, i);
                            ue.complete(i);
                        }
                    });
                    if (userData == IOUring.NO_SQE) {
                        end.set(true);
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }
                    ue.onTermination(() -> onTermination(end, userData));
                }));
    }

    public Uni<Integer> connectLazy() {
        if (!state.compareAndSet(INIT, CONNECTING)) {
            int stateSnap = state.get();
            if (stateSnap == CONNECTING || stateSnap == CONNECTED) {
                throw new IllegalStateException("already connected");
            }

            if (stateSnap == CLOSE) {
                throw new IllegalStateException("Socket is closed");
            }
        }

        return Uni.createFrom()
                .emitter(ue -> eventLoop.runOnEventLoop(() -> {

                    AtomicBoolean end = new AtomicBoolean(false);
                    try {
                        long userData = ring.prep_connect_and_get_user_data(new SocketInfo(fd, host, port), i -> {
                            if (!end.compareAndSet(false, true)) {
                                return;
                            }
                            if (i < 0) {
                                ue.fail(new NativeCallException(NativeHelper.getErrorStr(-i)));
                            } else {
                                state.compareAndSet(CONNECTING, CONNECTED);
                                ue.complete(i);
                            }
                        });
                        if (userData == IOUring.NO_SQE) {
                            end.set(true);
                            ue.fail(new NotEnoughSqeException());
                            return;
                        }
                        ue.onTermination(() -> onTermination(end, userData));
                    } catch (UnknownHostException e) {
                        ue.fail(e);
                    }
                }));
    }


    @Deprecated
    public CompletableFuture<Integer> connect() {
        if (!state.compareAndSet(INIT, CONNECTING)) {
            int stateSnap = state.get();
            if (stateSnap == CONNECTING || stateSnap == CONNECTED) {
                throw new IllegalStateException("already connected");
            }

            if (stateSnap == CLOSE) {
                throw new IllegalStateException("Socket is closed");
            }
            throw new IllegalStateException("current state dont support connect");
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            try {
                if (!ring.prep_connect(new SocketInfo(fd, host, port), info -> {
                    state.compareAndSet(CONNECTING, CONNECTED);
                    future.complete(info);
                })) {
                    future.completeExceptionally(new NotEnoughSqeException());
                }
            } catch (UnknownHostException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Deprecated
    public CompletableFuture<Integer> write(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (state.get() != CONNECTED) {
            throw new IllegalStateException("Socket is not connected");
        }
        Arena session = Arena.ofShared();
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

    public Uni<Integer> writeLazy(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return Uni.createFrom()
                .emitter(ue -> eventLoop.runOnEventLoop(() -> {
                    if (state.get() != CONNECTED) {
                        ue.fail(new IllegalStateException("Socket is not connected"));
                        return;
                    }
                    AtomicBoolean end = new AtomicBoolean(false);
                    Arena session = Arena.ofConfined();
                    MemorySegment memorySegment = session.allocate(length);
                    MemorySegment.copy(buffer, offset, memorySegment, JAVA_BYTE, 0, length);
                    long userData = ring.prep_send_and_get_user_data(fd, memorySegment, i -> {
                        session.close();
                        if (!end.compareAndSet(false, true)) {
                            return;
                        }
                        if (i < 0) {
                            ue.fail(new NativeCallException(NativeHelper.getErrorStr(-i)));
                        } else {
                            ue.complete(i);
                        }
                    });
                    if (userData == IOUring.NO_SQE) {
                        end.set(true);
                        session.close();
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }
                    ue.onTermination(() -> onTermination(end, userData));
                }));

    }

    @Deprecated
    public CompletableFuture<Integer> write(byte[] buffer) {
        return write(buffer, 0, buffer.length);
    }

    public Uni<Integer> writeLazy(byte[] buffer) {
        return writeLazy(buffer, 0, buffer.length);
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
    public boolean closed() {
        return state.get() == CLOSE;
    }

    @Override
    public void close() {
        int res = state.getAndAdd(CLOSE);
        if (res == CLOSE) {
            return;
        }
        eventLoop.runOnEventLoop(() -> {
            unistd_h.close(fd);
        });
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }

    @Override
    public int readFd() {
        return fd;
    }

    protected void checkStateBeforeSingleRecv() {
        if (state.get() != CONNECTED) {
            throw new IllegalStateException("Socket is not connected");
        }
        if (multiMode.get()) {
            throw new IllegalStateException("current in multi mode");
        }
    }
}
