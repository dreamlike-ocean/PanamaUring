package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.lang.foreign.MemorySegment;
import java.util.Optional;

public interface IoUringAsyncFd extends NativeFd, IoUringOperator {

    IoUringEventLoop owner();

    default CancelableFuture<BufferResult<OwnershipMemory>> asyncRead(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<BufferResult<OwnershipMemory>>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()))
                .whenComplete(buffer::DropWhenException);
    }

    default CancelableFuture<IoUringCqe> asyncSelectedRead(int len, int offset, short bufferGroupId) {
        return owner()
                .asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
                    sqe.setBufGroup(bufferGroupId);
                });
    }

    default CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>> asyncReadV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        IoUringEventLoop eventLoop = owner();
        return (CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>>) eventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_readv(sqe, readFd(), iovec.resource(), nr_vecs, offset))
                .whenComplete((_, t) -> Optional.ofNullable(t).ifPresent((_) -> iovec.drop()))
                .thenApply((cqe) -> new BufferResult<>(iovec, cqe.getRes()));
    }

    default CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>> asyncWriteV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        IoUringEventLoop eventLoop = owner();
        return (CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>>) eventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_writev(sqe, writeFd(), iovec.resource(), nr_vecs, offset))
                .whenComplete((_, t) -> Optional.ofNullable(t).ifPresent((_) -> iovec.drop()))
                .thenApply((cqe) -> new BufferResult<>(iovec, cqe.getRes()));
    }

    default CancelableFuture<BufferResult<OwnershipMemory>> asyncWrite(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<BufferResult<OwnershipMemory>>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_write(sqe, writeFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()))
                .whenComplete(buffer::DropWhenException);

    }
}
