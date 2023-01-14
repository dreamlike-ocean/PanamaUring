package top.dreamlike.async.file;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.open;
public class AsyncFile {
    private final IOUring uring;
    private final int fd;

    private final IOUringEventLoop eventLoop;


    public AsyncFile(String path,IOUringEventLoop eventLoop, int ops){
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment filePath = allocator.allocateUtf8String(path);
            fd = open(filePath,ops);
            if (fd < 0){
                throw new IllegalStateException("fd open error:"+ NativeHelper.getNowError());
            }

        }
        this.eventLoop = eventLoop;
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
    }


    /**
     *
     * @param offset 读取文件的偏移量
     * @param memorySegment 要保证有效的memory
     * @return 读取了多少字节
     */
    @Unsafe("memory segment要保证有效")
    public CompletableFuture<Integer> read(int offset, MemorySegment memorySegment){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_read(fd, offset, memorySegment, future::complete)){
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }


    public CompletableFuture<byte[]> read(int offset, int length){
        MemorySession memorySession = MemorySession.openShared();
        MemorySegment buffer = memorySession.allocate(length);
        return read(offset, buffer)
                .thenApply(i ->{
                    byte[] bytes = buffer.asSlice(0, i).toArray(JAVA_BYTE);
                    memorySession.close();
                    return bytes;
                } );
    }

    public CompletableFuture<byte[]> readSelected(int offset, int length){
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
           if (!uring.prep_selected_read(fd, offset, length, future)) {
               future.completeExceptionally(new Exception("没有空闲的sqe"));
           }
       });
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
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_write(fd, fileOffset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future.whenComplete((res,t) -> {
            session.close();
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
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_write(fd, offset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public CompletableFuture<Integer> fsync(){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_fsync(fd, 0, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public void close() {
        unistd_h.close(fd);
    }

    static {
        AccessHelper.fetchFileFd = (f) -> f.fd;
    }

}
