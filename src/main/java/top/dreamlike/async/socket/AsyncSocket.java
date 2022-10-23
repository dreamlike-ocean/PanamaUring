package top.dreamlike.async.socket;

import top.dreamlike.async.IOUring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
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
        ring.prep_selected_recv(fd, size, completableFuture);
        ring.submit();
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
        ring.prep_send(fd,offset, memorySegment, future::complete);
        ring.submit();
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
}
