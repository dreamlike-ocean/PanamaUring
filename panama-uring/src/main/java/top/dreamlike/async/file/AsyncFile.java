package top.dreamlike.async.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.PlainAsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.dreamlike.nativeLib.errno.errno_h.EWOULDBLOCK;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.flock.file_h.flock;

public class AsyncFile extends PlainAsyncFd {
    private static final Logger log = LoggerFactory.getLogger(AsyncFile.class);

    private final IOUring uring;
    private final int fd;

    private final AtomicBoolean hasLocked = new AtomicBoolean(false);


    public AsyncFile(String path, IOUringEventLoop eventLoop, int ops) {
        super(eventLoop);
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment filePath = allocator.allocateUtf8String(path);
            fd = open(filePath, ops);
            if (fd < 0) {
                throw new IllegalStateException("fd open error:" + NativeHelper.getNowError());
            }

        }
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
    }

    public AsyncFile(int fd,IOUringEventLoop eventLoop){
        super(eventLoop);
        this.fd = fd;
        if (fd < 0){
            throw new IllegalStateException("fd open error:"+ NativeHelper.getNowError());
        }
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
    }


    @Unsafe("memory segment要保证有效且为share范围的session")
    public CompletableFuture<Integer> readUnsafe(int offset, MemorySegment memorySegment) {
        return super.readUnsafe(offset, memorySegment);
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


    @Override
    public CompletableFuture<Integer> writeUnsafe(int offset, MemorySegment memorySegment) {
        return super.writeUnsafe(offset, memorySegment);
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


    @Override
    protected int readFd() {
        return fd;
    }

    static {
        AccessHelper.fetchFileFd = (f) -> f.fd;
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }

    public boolean tryLock() {
        int res = flock(fd, LOCK_EX() | LOCK_NB());
        if (res == 0) {
            hasLocked.set(true);
            return true;
        }
        if (NativeHelper.getErrorNo() == EWOULDBLOCK()) {
            return false;
        }
        throw new NativeCallException(NativeHelper.getNowError());
    }

    public boolean tryUnLock() {
        if (!hasLocked.get()) {
            return false;
        }

        int res = flock(fd, LOCK_NB() | LOCK_UN());

        if (res == 0) {
            hasLocked.set(false);
            return true;
        }
        throw new NativeCallException(NativeHelper.getNowError());
    }


    public CompletableFuture<Boolean> lock() {
        if (hasLocked.get()) {
            return CompletableFuture.completedFuture(true);
        }

        //fast path
        if (tryLock()) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> res = new CompletableFuture<>();
        lock0(res, Duration.ofMillis(1000));
        return res.whenComplete((__, t) -> {
            if (t instanceof CancellationException) {
                tryUnLock();
            }
        });
    }


    private void lock0(CompletableFuture<Boolean> completableFuture, Duration duration) {
        boolean b = tryLock();
        if (b) {
            completableFuture.complete(true);
            return;
        }
        eventLoop.scheduleTask(() -> lock0(completableFuture, duration), duration);
    }

}
