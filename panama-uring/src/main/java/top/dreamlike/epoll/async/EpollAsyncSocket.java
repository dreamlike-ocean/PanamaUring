package top.dreamlike.epoll.async;

import io.smallrye.mutiny.Uni;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.async.socket.extension.EpollFlow;
import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;
import top.dreamlike.nativeLib.epoll.epoll_h;
import top.dreamlike.nativeLib.inet.inet_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 基于epoll的aysnc socket 不走io_uring
 * 会挂到io_uring eventLoop上面
 */
public class EpollAsyncSocket extends AsyncSocket {


    private final Queue<Buffer> sendBuffers = new ArrayDeque<>();

    private int event = 0;


    public EpollAsyncSocket(int fd, String host, int port, EpollUringEventLoop eventLoop) {
        super(fd, host, port, eventLoop);
        NativeHelper.makeNoBlocking(fd);
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
    public CompletableFuture<Long> recv(byte[] buffer) {
        checkStateBeforeRecv();
        return eventLoop.runOnEventLoop(promise -> {
            event = event | epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT();
            fetchEventLoop().modifyAll(fd, epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT(), event -> {
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
    public Uni<Integer> recvLazy(byte[] buffer) {
        checkStateBeforeRecv();
        return Uni.createFrom().emitter(ue -> eventLoop.runOnEventLoop(() -> {
            AtomicBoolean isTerminated = new AtomicBoolean(false);
            //如果先取消这里会先比回调执行 不任何处理 oneshot模式激活一次 无所谓。。。
            ue.onTermination(() -> isTerminated.compareAndSet(false, true));
            event = event | epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT();
            fetchEventLoop().modifyAll(fd, epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT(), event -> {
                if (isTerminated.compareAndSet(false, true)) {
                    this.event &= ~(epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT());
                    try (Arena session = Arena.ofConfined()) {
                        MemorySegment buff = session.allocate(buffer.length);
                        long res = inet_h.recv(fd, buff, buffer.length, 0);
                        MemorySegment.ofArray(buffer).copyFrom(buff.reinterpret(res));
                        ue.complete((int) res);
                    }
                }
            });
        }));
    }

    @Override
    public Flow.Subscription recvMulti(Consumer<byte[]> consumer) {
        if (state.get() != CONNECTED) {
            throw new IllegalStateException("Socket is not connected");
        }
        if (!multiMode.compareAndSet(false, true)) {
            throw new IllegalStateException("current in multi mode");
        }
        EpollFlow<byte[]> flow = new EpollFlow<>(Integer.MAX_VALUE, fd, fetchEventLoop(), consumer);
        eventLoop
                .runOnEventLoop(() -> {
                    event = event | epoll_h.EPOLLIN();
                    fetchEventLoop().modifyAll(fd, epoll_h.EPOLLIN(), event -> {
                        try (Arena session = Arena.ofConfined()) {
                            MemorySegment buff = session.allocate(1024 * 4);
                            long res = inet_h.recv(fd, buff, buff.byteSize(), 0);
                            if (res < 0) {
                                deRegisteredOpDirectly();
                                flow.onError(new NativeCallException(NativeHelper.getNowError()));
                                return;
                            }
                            byte[] heap = new byte[1024 * 4];
                            MemorySegment.ofArray(heap).copyFrom(buff.reinterpret(res));
                            flow.onNext(heap);
                            if (flow.isFull()) {
                                deRegisteredOpDirectly();
                                multiMode.compareAndSet(true, false);
                            }
                        }
                    });
                });

        return flow;
    }

    @Override
    public CompletableFuture<Integer> connect() {
        if (connected.get()) {
            return CompletableFuture.completedFuture(0);
        }
        CompletableFuture<Integer> conncetFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {

            try (Arena session = Arena.ofConfined()) {
                Pair<MemorySegment, Boolean> socketInfo = null;
                try {
                    socketInfo = NativeHelper.getSockAddr(session, host, port);
                } catch (UnknownHostException e) {
                    conncetFuture.completeExceptionally(e);
                    return;
                }
                MemorySegment sockaddrSegement = socketInfo.t1();
                int res = inet_h.connect(fd, sockaddrSegement, (int) sockaddrSegement.byteSize());
                if (res == 0) {
                    connected.set(true);
                    conncetFuture.complete(0);
                    return;
                }
            }

        });
        return conncetFuture;
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

    protected void deRegisteredOpDirectly() {
        this.event &= ~(epoll_h.EPOLLIN() | epoll_h.EPOLLONESHOT());
        fetchEventLoop().modifyEvent(fd, this.event);
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

    @Override
    public String toString() {
        return super.toString();
    }
}
