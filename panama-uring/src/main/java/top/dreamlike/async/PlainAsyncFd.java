package top.dreamlike.async;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.NotEnoughSqeException;
import top.dreamlike.extension.memory.OwnershipMemory;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Uni;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public abstract non-sealed class PlainAsyncFd extends AsyncFd {
    protected IOUring ioUring;

    protected final AtomicBoolean closed = new AtomicBoolean(false);

    // 写的奇形怪状的防止冲突
    private static final String USER_DATA_KEY = "_USER_DATA_KEY_";

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
        Arena arena = Arena.openShared();
        MemorySegment buffer = arena.allocate(length);
        return readUnsafe(offset, buffer)
                .thenCompose(i -> i < 0
                        ? CompletableFuture.failedStage(new NativeCallException(NativeHelper.getErrorStr(-i)))
                        : CompletableFuture.completedFuture(i))
                .thenApply(i -> {
                    byte[] bytes = buffer.asSlice(0, i).toArray(JAVA_BYTE);
                    arena.close();
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
                future.completeExceptionally(new NotEnoughSqeException());
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

        Arena session = Arena.openShared();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        MemorySegment memorySegment = session.allocate(bufferLength);
        MemorySegment.copy(buffer, bufferOffset, memorySegment, JAVA_BYTE, 0, bufferLength);
        eventLoop.runOnEventLoop(() -> {
            if (!ioUring.prep_write(writeFd(), fileOffset, memorySegment, future::complete)) {
                future.completeExceptionally(new NotEnoughSqeException());
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
                future.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return future;
    }

    /**
     * 
     * @param offset        文件偏移量
     * @param memorySegment 所有权移交给io_uring语义的内存段
     * @return
     */
    protected Uni<Integer> writeUnsafeLazy(int offset, OwnershipMemory memorySegment) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        Uni<Integer> lazyRes = Uni.createFrom()
                .emitter((ue) -> {
                    AtomicBoolean normalEnd = new AtomicBoolean(false);
                    long userData = ioUring.prep_write_and_get_user_data(writeFd(), offset, memorySegment.resource(),
                            (writeRes) -> {
                                // 先确保下游拿到memorysegment’再释放
                                var casResult = normalEnd.compareAndExchange(false, true);
                                ue.complete(writeRes);
                                if (casResult) {
                                    memorySegment.drop();
                                }
                            });

                    if (userData == IOUring.NO_SQE) {
                        normalEnd.set(true);
                        memorySegment.drop();
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }

                    ue.onTermination(() -> {

                        if (normalEnd.get()) {
                            return;
                        }
                        // 非正常结束比如说cancel了
                        eventLoop.cancelAsync(userData, 0, true)
                                .subscribe().with(i -> {
                                    normalEnd.set(true);
                                    memorySegment.drop();
                                }, t -> {
                                    // todo 这里异常被吞了。。。
                                    // 取消失败 不释放资源等待async op回调
                                });

                    });

                });

        return lazyRes.withContext((uni, c) -> {
            Long userData = c.get(USER_DATA_KEY);
            return uni.onCancellation()
                    .call(() -> eventLoop.cancelAsync(userData, 0, true));
        });
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
