package top.dreamlike.asyncFile;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
public class AsyncFile {
    IOUring uring;
    int fd;


    AsyncFile(String path,IOUring uring,int ops){
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment filePath = allocator.allocateUtf8String(path);
            fd = open(filePath,ops);
            if (fd < 0){
                throw new IllegalStateException("fd open error");
            }
        }
        this.uring = uring;
    }


    public CompletableFuture<Integer> read(int offset,MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        uring.prep_readV(fd,offset, memorySegment, future::complete);
        uring.submit();
        return future;
    }


    public CompletableFuture<byte[]> read(int offset,int length){
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        uring.selectedRead(fd,offset,length, future);
        uring.submit();
        return future;
    }



    public CompletableFuture<Integer> write(int offset,MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        uring.prep_writeV(fd,offset, memorySegment, future::complete);
        uring.submit();
        return future;
    }

}
