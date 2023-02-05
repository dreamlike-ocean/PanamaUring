package top.dreamlike.async;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public abstract non-sealed class PlainAsyncFd extends AsyncFd {
    protected IOUring ioUring;

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected PlainAsyncFd(IOUringEventLoop eventLoop) {
        super(eventLoop);
        ioUring = AccessHelper.fetchIOURing.apply(eventLoop);
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }


    public CompletableFuture<byte[]> read(int offset, int length) {
        if (closed()) {
            throw new NativeCallException("file has closed");
        }
        MemorySession memorySession = MemorySession.openShared();
        MemorySegment buffer = memorySession.allocate(length);
        return readUnsafe(offset, buffer)
                .thenCompose(i -> i < 0 ? CompletableFuture.failedStage(new NativeCallException(NativeHelper.getErrorStr(-i))) : CompletableFuture.completedFuture(i))
                .thenApply(i -> {
                    byte[] bytes = buffer.asSlice(0, i).toArray(JAVA_BYTE);
                    memorySession.close();
                    return bytes;
                });
    }

    /**
     * @param offset        读取文件的偏移量
     * @param memorySegment 要保证有效的memory
     * @return 读取了多少字节
     */
    protected CompletableFuture<Integer> readUnsafe(int offset, MemorySegment memorySegment) {
        if (closed()) {
            throw new NativeCallException("file has closed");
        }

        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ioUring.prep_read(readFd(), offset, memorySegment, future::complete)) {
                future.completeExceptionally(new NativeCallException("没有空闲的sqe"));
            }
        });
        return future;
    }

    public CompletableFuture<Integer> write(int fileOffset, byte[] buffer, int bufferOffset, int bufferLength) {
        if (bufferOffset + bufferLength > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (closed()) {
            throw new NativeCallException("file has closed");
        }

        MemorySession session = MemorySession.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(bufferLength);
        MemorySegment.copy(buffer, bufferOffset, memorySegment, JAVA_BYTE, 0, bufferLength);
        eventLoop.runOnEventLoop(() -> {
            if (!ioUring.prep_write(writeFd(), fileOffset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future.whenComplete((res, t) -> {
            session.close();
        });
    }

    /**
     * @param offset        文件偏移量
     * @param memorySegment 需要调用者保证一直有效
     * @return
     */
    protected CompletableFuture<Integer> writeUnsafe(int offset, MemorySegment memorySegment) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ioUring.prep_write(writeFd(), offset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public boolean closed() {
        return closed.get();
    }

    protected abstract int readFd();


    protected void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                unistd_h.close(readFd());
                if (readFd() != writeFd()) {
                    unistd_h.close(writeFd());
                }
            } catch (RuntimeException e) {
                closed.set(false);
                throw e;
            }
        }
    }

    protected int writeFd() {
        return readFd();
    }
}
