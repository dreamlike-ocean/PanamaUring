package top.dreamlike.async.file;

import top.dreamlike.async.uring.IOUring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
public class AsyncFile {
    IOUring uring;
    int fd;


    public AsyncFile(String path,IOUring uring,int ops){
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment filePath = allocator.allocateUtf8String(path);
            fd = open(filePath,ops);
            if (fd < 0){
                throw new IllegalStateException("fd open error");
            }
        }
        this.uring = uring;
    }


    /**
     *
     * @param offset 读取文件的偏移量
     * @param memorySegment 要保证有效的memory
     * @return 读取了多少字节
     */
    public CompletableFuture<Integer> read(int offset,MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        uring.prep_readV(fd,offset, memorySegment, future::complete);
        return future;
    }


    public CompletableFuture<byte[]> read(int offset,int length){
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        uring.prep_selected_read(fd,offset,length, future);
        return future;
    }



    public CompletableFuture<Integer> write(int fileOffset,byte[] buffer,int bufferOffset,int bufferLength){
        if (bufferOffset + bufferLength > buffer.length){
            throw new ArrayIndexOutOfBoundsException();
        }
        MemorySession session = MemorySession.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(bufferLength);
        MemorySegment.copy(buffer, bufferOffset, memorySegment, JAVA_BYTE, 0, bufferLength);
        uring.prep_write(fd,fileOffset, memorySegment, future::complete);
        return future.thenApply( res -> {
            session.close();
            return res;
        });
    }


    /**
     *
     * @param offset 文件偏移量
     * @param memorySegment 需要调用者保证一直有效
     * @return
     */
    public CompletableFuture<Integer> write(int offset,MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        uring.prep_write(fd,offset, memorySegment, future::complete);
        return future;
    }

}
