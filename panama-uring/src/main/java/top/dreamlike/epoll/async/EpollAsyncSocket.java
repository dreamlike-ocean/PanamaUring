package top.dreamlike.epoll.async;

import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;
import top.dreamlike.nativeLib.socket.socket_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLOUT;
import static top.dreamlike.nativeLib.errno.errno_h.EAGAIN;

/**
 * 基于epoll的aysnc socket 不走io_uring
 * 会挂到io_uring eventLoop上面
 */
public class EpollAsyncSocket extends AsyncSocket {

    /**
     * multi shot模式下的缓冲
     */
    private final Queue<Buffer> recvBuffers;

    private final Queue<Runnable> recvQuest;

    private final AtomicInteger maxReadBufferQueueLength;

    private final Queue<Buffer> sendBuffers = new ArrayDeque<>();

    private int event = 0;

    private final static int recvSize = 1024;

    private final AtomicBoolean connected;

    public EpollAsyncSocket(int fd, String host, int port, EpollUringEventLoop eventLoop) {
        super(fd, host, port, eventLoop);
        recvBuffers = new ArrayDeque<>();
        recvQuest = new ArrayDeque<>();
        maxReadBufferQueueLength = new AtomicInteger(10);
        NativeHelper.makeNoBlocking(fd);
        connected = new AtomicBoolean(true);
    }

    public EpollAsyncSocket(SocketAddress address, IOUringEventLoop eventLoop) {
        super(address, eventLoop);
        recvBuffers = new ArrayDeque<>();
        recvQuest = new ArrayDeque<>();
        maxReadBufferQueueLength = new AtomicInteger(10);
        NativeHelper.makeNoBlocking(fd);
        connected = new AtomicBoolean(false);
    }

    @Override
    public EpollUringEventLoop fetchEventLoop() {
        return (EpollUringEventLoop) super.fetchEventLoop();
    }

    @Override
    public CompletableFuture<byte[]> recvSelected(int expectSize) {
        byte[] res = new byte[expectSize];
        return recv(res)
                .thenApply(i -> Arrays.copyOfRange(res, 0, i));
    }

    @Override
    public CompletableFuture<Integer> recv(byte[] buffer) {
        if (!connected.get()) {
            throw new NativeCallException("must connect first");
        }
        CompletableFuture<Integer> recvFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            //fast path
            if (!recvBuffers.isEmpty()) {
                int res = readFromBuffer(buffer);
                recvFuture.complete(res);
                return;
            }

            recvQuest.offer(() -> {
                int res = readFromBuffer(buffer);
                recvFuture.complete(res);
            });

            if ((event & EPOLLIN()) == 0) {
                fetchEventLoop().modifyAll(fd, event | EPOLLIN(), this::handleEvent);
            }
        });

        return recvFuture;
    }


    @Override
    public CompletableFuture<Integer> connect() {
        if (connected.get()) {
            return CompletableFuture.completedFuture(0);
        }
        CompletableFuture<Integer> conncetFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {

            try (Arena session = Arena.openConfined()) {
                Pair<MemorySegment, Boolean> socketInfo = null;
                try {
                    socketInfo = NativeHelper.getSockAddr(session, host, port);
                } catch (UnknownHostException e) {
                    conncetFuture.completeExceptionally(e);
                    return;
                }
                MemorySegment sockaddrSegement = socketInfo.t1();
                int res = socket_h.connect(fd, sockaddrSegement, (int) sockaddrSegement.byteSize());
                if (res == 0) {
                    connected.set(true);
                    conncetFuture.complete(0);
                    return;
                }
            }

            recvQuest.offer(() -> {
                connected.set(true);
                conncetFuture.complete(0);
                fetchEventLoop().modifyEvent(fd, 0);
                event = 0;
            });
            fetchEventLoop().registerEvent(fd, EPOLLIN(), this::handleEvent);
            event |= EPOLLIN();
        });
        return conncetFuture;
    }

    private void handleEvent(int event) {
        if ((event & EPOLLIN()) != 0) {
            if (connected.get()) {
                multiShotModeRead();
            } else {
                recvQuest.poll().run();
            }
        }

        //写读事件
        if ((event & EPOLLOUT()) != 0) {
            while (!sendBuffers.isEmpty()) {
                Buffer peek = sendBuffers.peek();
                int expectWrite = peek.base.length - peek.offset;
                //就是在eventloop上面
                Integer res = write(peek.base, peek.offset, expectWrite).resultNow();
                if (res < expectWrite) {
                    break;
                }
            }

            event &= ~EPOLLOUT();
            fetchEventLoop().modifyEvent(fd, event);
        }

    }

    private int readFromBuffer(byte[] buffer) {
        var recvBuffer = this.recvBuffers.peek();
        int res = Math.min(recvBuffer.base.length - recvBuffer.offset, buffer.length);
        System.arraycopy(recvBuffer.base, recvBuffer.offset, buffer, 0, res);
        recvBuffer.offset += res;
        if (recvBuffer.offset == recvBuffer.base.length) {
            this.recvBuffers.poll();
        }
        return res;
    }

    @Override
    public CompletableFuture<Integer> write(byte[] buffer, int offset, int length) {
        return eventLoop.runOnEventLoop(() -> {
            if (!sendBuffers.isEmpty()) {
                sendBuffers.offer(new Buffer(buffer, 0));
                return 0;
            }
            try (Arena session = Arena.openConfined()) {
                MemorySegment writeBuffer = session.allocate(length - offset);
                writeBuffer.copyFrom(MemorySegment.ofArray(buffer).asSlice(offset));
                long sendRes = socket_h.send(fd, writeBuffer, length - offset, 0);
                if (sendRes < length - offset) {
                    event |= EPOLLOUT();
                    fetchEventLoop().modifyEvent(fd, event);
                    sendBuffers.offer(new Buffer(buffer, offset + length));
                }
                return (int) sendRes;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> write(byte[] buffer) {
        return write(buffer, 0, buffer.length);
    }

    public void setMaxReadBufferQueueLength(int maxReadBufferQueueLength) {
        if (maxReadBufferQueueLength < 1) {
            throw new IllegalArgumentException("too small queueLength");
        }
        this.maxReadBufferQueueLength.setRelease(maxReadBufferQueueLength);
    }


    private void multiShotModeRead() {
        try (Arena session = Arena.openConfined()) {
            MemorySegment buff = session.allocate(recvSize);
            long recv = socket_h.recv(fd, buff, recvSize, 0);
            if (recv < 0 && NativeHelper.getErrorNo() == EAGAIN()) {
                return;
            }

            byte[] base = buff.asSlice(0, recv).toArray(JAVA_BYTE);
            recvBuffers.offer(new Buffer(base, 0));
            if (recvBuffers.size() == maxReadBufferQueueLength.get()) {
                // 停止监听 被压策略
                event &= ~EPOLLIN();
                fetchEventLoop().modifyEvent(fd, event);
            }
            //存在时间顺序 不会npe
            var poll = recvQuest.poll();
            poll.run();

        }
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
