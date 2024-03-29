package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;

public interface IoUringAsyncFd extends NativeFd {

    IoUringEventLoop owner();

    default CancelableFuture<Integer> asyncRead(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<Integer>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .thenApply(IoUringCqe::getRes)
                .whenComplete((_, _) -> buffer.drop());
    }

    default CancelableFuture<IoUringCqe> asyncSelectedRead(int len, int offset, short bufferGroupId) {
        return owner()
                .asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
                    sqe.setBufGroup(bufferGroupId);
                });
    }

    default CancelableFuture<Integer> asyncWrite(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<Integer>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_write(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .thenApply(IoUringCqe::getRes)
                .whenComplete((_, _) -> buffer.drop());
    }
}
