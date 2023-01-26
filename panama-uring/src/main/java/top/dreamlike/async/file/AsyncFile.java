package top.dreamlike.async.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.open;

public non-sealed class AsyncFile implements AsyncFd, EventLoopAccess {
    private static final Logger log = LoggerFactory.getLogger(AsyncFile.class);

    private final IOUring uring;
    private final int fd;

    private final IOUringEventLoop eventLoop;

    private final AtomicBoolean closed = new AtomicBoolean(false);


    public AsyncFile(String path, IOUringEventLoop eventLoop, int ops) {
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment filePath = allocator.allocateUtf8String(path);
            fd = open(filePath, ops);
            if (fd < 0) {
                throw new IllegalStateException("fd open error:" + NativeHelper.getNowError());
            }

        }
        this.eventLoop = eventLoop;
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
    }

    public AsyncFile(int fd,IOUringEventLoop eventLoop){
        this.fd = fd;
        if (fd < 0){
            throw new IllegalStateException("fd open error:"+ NativeHelper.getNowError());
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
    @Unsafe("memory segment要保证有效且为share范围的session")
    public CompletableFuture<Integer> read(int offset, MemorySegment memorySegment) {
        MemorySession memorySession = memorySegment.session();
        if (!memorySession.isAlive() || memorySession.ownerThread() != null) {
            throw new NativeCallException("illegal memory segment");
        }

        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }

        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_read(fd, offset, memorySegment, future::complete)) {
                future.completeExceptionally(new NativeCallException("没有空闲的sqe"));
            }
        });
        return future;
    }


    public CompletableFuture<byte[]> read(int offset, int length) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        MemorySession memorySession = MemorySession.openShared();
        MemorySegment buffer = memorySession.allocate(length);
        return read(offset, buffer)
                .thenCompose(i -> i < 0 ? CompletableFuture.failedStage(new NativeCallException(NativeHelper.getErrorStr(-i))) : CompletableFuture.completedFuture(i))
                .thenApply(i -> {
                    byte[] bytes = buffer.asSlice(0, i).toArray(JAVA_BYTE);
                    memorySession.close();
                    return bytes;
                });
    }

    public CompletableFuture<byte[]> readSelected(int offset, int length) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_selected_read(fd, offset, length, future)) {
                future.completeExceptionally(new NativeCallException("没有空闲的sqe"));
            }
        });
        return future;
    }

    public CompletableFuture<Integer> write(int fileOffset, byte[] buffer, int bufferOffset, int bufferLength) {
        if (bufferOffset + bufferLength > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (closed.get()) {
            throw new NativeCallException("file has closed");
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
     * @param offset        文件偏移量
     * @param memorySegment 需要调用者保证一直有效
     * @return
     */
    public CompletableFuture<Integer> write(int offset, MemorySegment memorySegment) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_write(fd, offset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public CompletableFuture<Integer> fsync() {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_fsync(fd, 0, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                unistd_h.close(fd);
            } catch (RuntimeException e) {
                closed.set(false);
                throw e;
            }
        }
    }

    public boolean closed() {
        return closed.get();
    }

    static {
        AccessHelper.fetchFileFd = (f) -> f.fd;
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }
}
