package top.dreamlike.async;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.concurrent.CompletableFuture;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public abstract non-sealed class PlainAsyncFd extends AsyncFd {
    protected IOUring ioUring;

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
        return read(offset, buffer)
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
    public CompletableFuture<Integer> read(int offset, MemorySegment memorySegment) {
        if (closed()) {
            throw new NativeCallException("file has closed");
        }

        CompletableFuture<Integer> future = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!ioUring.prep_read(fd(), offset, memorySegment, future::complete)) {
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
            if (!ioUring.prep_write(fd(), fileOffset, memorySegment, future::complete)) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future.whenComplete((res, t) -> {
            session.close();
        });
    }

    public abstract boolean closed();

    protected abstract int fd();

}
