package top.dreamlike.epoll.async;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.eventloop.EpollEventLoop;
import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;
import top.dreamlike.nativeLib.epoll.epoll_h;
import top.dreamlike.nativeLib.inet.inet_h;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于epoll的aysnc socket 不走io_uring
 * 会挂到io_uring eventLoop上面
 */
public class EpollAsyncSocket extends AsyncSocket {


    private final Queue<Buffer> sendBuffers = new ArrayDeque<>();

    private int event = 0;

    public volatile int readBufferSize;


    public EpollAsyncSocket(int fd, String host, int port, EpollUringEventLoop eventLoop) {
        this(fd, host, port, eventLoop, 1024);
    }

    public EpollAsyncSocket(int fd, String host, int port, EpollUringEventLoop eventLoop, int readBufferSize) {
        super(fd, host, port, eventLoop);
        NativeHelper.makeNoBlocking(fd);
        this.readBufferSize = readBufferSize;
    }

    public EpollAsyncSocket(SocketAddress address, IOUringEventLoop eventLoop) {
        super(address, eventLoop);
        NativeHelper.makeNoBlocking(fd);
    }

    @Override
    public EpollUringEventLoop fetchEventLoop() {
        return (EpollUringEventLoop) super.fetchEventLoop();
    }

    @Override
    public CompletableFuture<byte[]> recvSelected(int expectSize) {
        byte[] res = new byte[expectSize];
        return recv(res)
                .thenApply(i -> Arrays.copyOfRange(res, 0, i.intValue()));
    }

    @Override
    @Deprecated
    public CompletableFuture<Long> recv(byte[] buffer) {
        checkStateBeforeSingleRecv();
        return eventLoop.runOnEventLoop(promise -> {
            event = event | epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT();
            fetchEventLoop().epollMode().registerReadEvent(fd, epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT(), event -> {
                try (Arena session = Arena.ofConfined()) {
                    this.event &= ~(epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT());
                    MemorySegment buff = session.allocate(buffer.length);
                    long res = inet_h.recv(fd, buff, buffer.length, 0);
                    MemorySegment.ofArray(buffer).copyFrom(buff.reinterpret(res));
                    promise.complete(res);
                }
            });
        });
    }

    @Override
    public Uni<Integer> recvLazy(byte[] buffer, boolean closeWhenEOF) {
        checkStateBeforeSingleRecv();
        return Uni.createFrom().emitter(ue -> eventLoop.runOnEventLoop(() -> {
            AtomicBoolean isTerminated = new AtomicBoolean(false);
            //如果先取消这里会先比回调执行 不任何处理 oneshot模式激活一次 无所谓。。。
            ue.onTermination(() -> isTerminated.compareAndSet(false, true));
            event = event | epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT();
            fetchEventLoop().epollMode().registerReadEvent(fd, event, event -> {
                this.event = event;
                if (isTerminated.compareAndSet(false, true)) {
                    try (Arena session = Arena.ofConfined()) {
                        MemorySegment buff = session.allocate(buffer.length);
                        long res = inet_h.recv(fd, buff, buffer.length, 0);
                        if (res == 0 && closeWhenEOF) {
                            if (state.compareAndSet(CONNECTED, CLOSE)) {
                                unistd_h.close(fd);
                            }
                            ue.fail(new IllegalStateException("socket has closed"));
                            return;
                        }
                        MemorySegment.ofArray(buffer).copyFrom(buff.reinterpret(res));
                        ue.complete((int) res);
                    }
                }
            });
        }));
    }

    @Override
    public Uni<byte[]> recvSelectLazy(int size) {
        byte[] buffer = new byte[size];
        return recvLazy(buffer)
                .map(i -> Arrays.copyOf(buffer, i));
    }

    /**
     * 若EOF则抛出异常
     *
     * @return
     */
    @Override
    public Multi<byte[]> recvMulti() {
        if (!multiMode.compareAndSet(false, true)) {
            throw new IllegalStateException("current state is in multiMode");
        }
        return Multi.createFrom().emitter(me -> eventLoop.runOnEventLoop(() -> {
            AtomicBoolean isTerminated = new AtomicBoolean(false);
            event = event | epoll_h.EPOLLIN();
            EpollUringEventLoop epollEventLoop = fetchEventLoop();
            epollEventLoop.epollMode().registerReadEvent(fd, event, event -> {
                this.event = event;
                try (Arena session = Arena.ofConfined()) {
                    int bufferSize = readBufferSize;
                    MemorySegment buff = session.allocate(bufferSize);
                    long res = inet_h.recv(fd, buff, bufferSize, 0);
                    if (res == 0) {
                        isTerminated.setRelease(true);
                        close();
                        me.fail(new SocketException("socket has been closed"));
                        return;
                    }
                    byte[] buffer = new byte[((int) res)];
                    MemorySegment.ofArray(buffer).copyFrom(buff.reinterpret(res));
                    me.emit(buffer);
                }
            });

            me.onTermination(() -> {
                if (!isTerminated.compareAndSet(false, true)) {
                    return;
                }
                fetchEventLoop().epollMode()
                        .removeEvent(fd, epoll_h.EPOLLIN());
            });

        }));
    }

    @Override
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
        CompletableFuture<Integer> conncetPromise = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {

            try (Arena session = Arena.ofConfined()) {
                Pair<MemorySegment, Boolean> socketInfo = null;
                try {
                    socketInfo = NativeHelper.getSockAddr(session, host, port);
                } catch (UnknownHostException e) {
                    conncetPromise.completeExceptionally(e);
                    return;
                }
                MemorySegment sockaddrSegement = socketInfo.t1();
                int res = inet_h.connect(fd, sockaddrSegement, (int) sockaddrSegement.byteSize());
                if (res == 0) {
                    state.compareAndSet(CONNECTING, CONNECTED);
                    conncetPromise.complete(0);
                    return;
                }
                event = event | epoll_h.EPOLLOUT();
                EpollEventLoop epoll = fetchEventLoop().epollMode();
                epoll.registerWriteEvent(fd, event, event -> {
                    //todo connect error handle
                    if (state.compareAndSet(CONNECTING, CONNECTED)) {
                        epoll.removeEvent(fd, epoll_h.EPOLLOUT());
                        this.event &= ~this.event;
                        conncetPromise.complete(0);
                    } else {
                        conncetPromise.completeExceptionally(new IllegalStateException("current state is not connecting"));
                    }
                });
            }

        });
        return conncetPromise;
    }

    @Override
    public Uni<Integer> connectLazy() {
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
        return Uni.createFrom()
                .emitter(ue -> {
                    AtomicBoolean end = new AtomicBoolean(false);
                    ue.onTermination(() -> {
                        if (!end.compareAndSet(false, true)) {
                            return;
                        }
                        close();
                    });
                    connect()
                            .whenComplete((i, t) -> {
                                boolean cancel = end.compareAndSet(false, true);
                                if (cancel) {
                                    return;
                                }
                                if (t != null) {
                                    ue.complete(i);
                                } else {
                                    close();
                                    ue.fail(t);
                                }
                            });
                });
    }

    @Override
    public CompletableFuture<Integer> write(byte[] buffer, int offset, int length) {
        return eventLoop.runOnEventLoop(() -> {
            if (!sendBuffers.isEmpty()) {
                sendBuffers.offer(new Buffer(buffer, 0));
                return 0;
            }
            try (Arena session = Arena.ofConfined()) {
                MemorySegment writeBuffer = session.allocate(length - offset);
                writeBuffer.copyFrom(MemorySegment.ofArray(buffer).asSlice(offset));
                long sendRes = inet_h.send(fd, writeBuffer, length - offset, 0);
                if (sendRes < length - offset) {
                    sendBuffers.offer(new Buffer(buffer, offset + length));
                }
                return (int) sendRes;
            }
        });
    }

    @Override
    public Uni<Integer> writeLazy(byte[] buffer, int offset, int length) {
        return Uni.createFrom()
                .completionStage(() -> write(buffer, offset, length));
    }

    @Override
    public CompletableFuture<Integer> write(byte[] buffer) {
        return write(buffer, 0, buffer.length);
    }



    private static class Buffer {
        byte[] base;
        int offset;

        public Buffer(byte[] base, int offset) {
            this.base = base;
            this.offset = offset;
        }
    }

    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    @Override
    public void close() {
        int res = state.getAndSet(CLOSE);
        if (res == CLOSE) {
            return;
        }
        eventLoop.runOnEventLoop(() -> {
            fetchEventLoop().epollMode()
                    .unregisterEvent(fd);
            unistd_h.close(fd);
        });
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
