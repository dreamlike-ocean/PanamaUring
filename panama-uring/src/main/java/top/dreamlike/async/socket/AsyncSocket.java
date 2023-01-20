package top.dreamlike.async.socket;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.net.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;


public non-sealed class AsyncSocket implements EventLoopAccess, AsyncFd {
    //todo 区分本地地址和远端地址
    // 目前是靠对于用途对host和port解释不同
    private final int fd;
    private final String host;
    private final int port;

    private final IOUring ring;

    private final IOUringEventLoop eventLoop;

    public AsyncSocket(int fd, String host, int port, IOUringEventLoop eventLoop) {
        this.fd = fd;
        this.host = host;
        this.port = port;
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
    }


    public AsyncSocket(SocketAddress address, IOUringEventLoop eventLoop) {
        this.ring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.eventLoop = eventLoop;
        switch (address) {
            case InetSocketAddress remote -> {
                host = remote.getHostString();
                port = remote.getPort();
                fd = remote.getAddress() instanceof Inet6Address ? NativeHelper.tcpClientSocketV6() : NativeHelper.tcpClientSocket();
            }
            case UnixDomainSocketAddress local -> {
                //todo uds有空再做
                throw new IllegalStateException("uds unsupported");
            }
            default -> throw new IllegalStateException("Unexpected value: " + address);
        }
    }


    public CompletableFuture<byte[]> readSelected(int size){
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ring.prep_selected_recv(fd, size, completableFuture)) {
                completableFuture.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return completableFuture;
    }

    public CompletableFuture<Integer> connect() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            try {
                if (!ring.prep_connect(new SocketInfo(fd,host,port), future::complete)){
                    future.completeExceptionally(new Exception("没有空闲的sqe"));
                }
            } catch (UnknownHostException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Integer> write(byte[] buffer,int offset,int length){
        if (offset + length > buffer.length){
            throw new ArrayIndexOutOfBoundsException();
        }
        MemorySession session = MemorySession.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(length);
        MemorySegment.copy(buffer, offset, memorySegment, JAVA_BYTE, 0, length);
        eventLoop.runOnEventLoop(() -> {
            if (!ring.prep_send(fd, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future.whenComplete((res,t) -> {
            session.close();
        });
    }



    @Override
    public String toString() {
        return "AsyncSocket{" +
                "fd=" + fd +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", ring=" + ring +
                '}';
    }


    public SocketAddress getAddress(){
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
