package top.dreamlike.async.socket;

import top.dreamlike.async.uring.IOUring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;


public class AsyncSocket {
    private final int fd;
    private final String host;
    private final int port;

    private final IOUring ring;

    public AsyncSocket(int fd, String host, int port,IOUring ring) {
        this.fd = fd;
        this.host = host;
        this.port = port;
        this.ring = ring;
    }

    public CompletableFuture<byte[]> read(int size){
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        if (!ring.prep_selected_recv(fd, size, completableFuture)) {
            completableFuture.completeExceptionally(new Exception("没有空闲的sqe"));
        }
        return completableFuture;
    }

    public CompletableFuture<Integer> write(byte[] buffer,int offset,int length){
        if (offset + length > buffer.length){
            throw new ArrayIndexOutOfBoundsException();
        }
        MemorySession session = MemorySession.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(length);
        MemorySegment.copy(buffer, offset, memorySegment, JAVA_BYTE, 0, length);
        if (!ring.prep_send(fd, memorySegment, future::complete)) {
            future.completeExceptionally(new Exception("没有空闲的sqe"));
        }
        return future.thenApply( res -> {
            session.close();
            return res;
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
}
