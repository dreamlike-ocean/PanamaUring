package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;

public interface IoUringAsyncFd extends NativeFd {

    IoUringEventLoop owner();

    default CancelableFuture<Integer> asyncRead(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<Integer>) owner()
                        .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                        .whenComplete((_, _) -> buffer.drop());
    }

    default CancelableFuture<Integer> asyncWrite(OwnershipMemory buffer, int len, int offset) {
        return (CancelableFuture<Integer>) owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_write(sqe, readFd(), MemorySegment.ofAddress(buffer.resource().address()), len, offset))
                .whenComplete((_, _) -> buffer.drop());
    }
}
