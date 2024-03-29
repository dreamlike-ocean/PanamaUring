package top.dreamlike.async;

import io.smallrye.mutiny.Uni;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.NotEnoughSqeException;
import top.dreamlike.extension.memory.OwnershipMemory;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Arena arena = Arena.ofShared();
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
            //防止有Confined的内存跨线程
            MemorySegment unsafeMemory = MemorySegment.ofAddress(memorySegment.address()).reinterpret(memorySegment.byteSize());
            if (!ioUring.prep_read(readFd(), offset, unsafeMemory, future::complete)) {
                future.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return future;
    }

    public Uni<Pair<OwnershipMemory, Integer>> readUnsafeLazy(int offset, OwnershipMemory memory) {
        if (closed()) {
            throw new NativeCallException("file has closed");
        }
        Uni<Pair<OwnershipMemory, Integer>> lazyRes = Uni.createFrom()
                .emitter(ue -> eventLoop.runOnEventLoop(() -> {
                    AtomicBoolean end = new AtomicBoolean(false);
                    long userData = ioUring.prep_read_and_get_user_data(readFd(), offset, memory.resource(), sysCallRes -> {
                        if (end.compareAndSet(false, true)) {
                            ue.complete(new Pair<>(memory, sysCallRes));
                        }else {
                            //async op被取消的场景
                            memory.drop();
                        }
                    });
                    if (userData == IOUring.NO_SQE) {
                        memory.drop();
                        end.set(true);
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }
                    ue.onTermination(() -> onTermination(end, userData));
                }));
        return lazyRes;
    }

    public CompletableFuture<Integer> write(int fileOffset, byte[] buffer, int bufferOffset, int bufferLength) {
        if (bufferOffset + bufferLength > buffer.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (closed()) {
            throw new NativeCallException("file has closed");
        }

        Arena session = Arena.ofShared();
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
            MemorySegment unsafeMemory = MemorySegment.ofAddress(memorySegment.address())
                    .reinterpret(memorySegment.byteSize());
            if (!ioUring.prep_write(writeFd(), offset, unsafeMemory, future::complete)) {
                future.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return future;
    }

    /**
     * 这里解决的是流的终止和MemorySegment所有权生命周期并不一致的问题
     * 只有成功回调才会将所有权释放回用户侧
     * @param offset        文件偏移量
     * @param memorySegment 所有权移交给io_uring语义的内存段
     * @return
     */
    public Uni<Pair<OwnershipMemory, Integer>> writeUnsafeLazy(int offset, OwnershipMemory memorySegment) {
        if (closed.get()) {
            throw new NativeCallException("file has closed");
        }
        Uni<Pair<OwnershipMemory, Integer>> lazyRes = Uni.createFrom()
                .emitter((ue) -> eventLoop.runOnEventLoop(() -> {

                    AtomicBoolean end = new AtomicBoolean(false);
                    long userData = ioUring.prep_write_and_get_user_data(writeFd(), offset, memorySegment.resource(), (sysCallRes) -> {
                                //即使cancel成功了 io_uring仍旧存在回调
                                // 转移所有权给下游
                        if (end.compareAndSet(false, true)) {
                            ue.complete(new Pair<>(memorySegment, sysCallRes));
                        } else {
                            //发现被取消了直接drop
                            memorySegment.drop();
                        }
                    });


                    if (userData == IOUring.NO_SQE) {
                        end.set(true);
                        memorySegment.drop();
                        ue.fail(new NotEnoughSqeException());
                        return;
                    }

                    ue.onTermination(() -> onTermination(end, userData));

                }));

        return lazyRes;
    }





}
