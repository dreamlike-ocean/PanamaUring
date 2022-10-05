package top.dreamlike.asyncFile;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
public class AsyncFile {
    IOUring uring;
    int fd;

    AsyncFile(String path,IOUring uring,int ops){
        MemorySegment filePath = uring.allocator.allocateUtf8String(path);
        fd = open(filePath,ops);
        if (fd < 0){
            throw new IllegalStateException("fd open error");
        }
        this.uring = uring;
    }


    public CompletableFuture<Integer> read(int offset,MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        uring.prep_readV(fd,offset, memorySegment, future::complete);
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
