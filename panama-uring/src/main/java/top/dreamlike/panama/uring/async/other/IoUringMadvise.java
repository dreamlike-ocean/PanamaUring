package top.dreamlike.panama.uring.async.other;

import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.LibMman;

import java.lang.foreign.MemorySegment;
import java.nio.MappedByteBuffer;

public class IoUringMadvise {

    public static CancelableFuture<IoUringSyscallResult<Void>> asyncMadive(IoUringEventLoop ioUringEventLoop, MemorySegment base, int advise) {
        if (!base.isMapped()) {
            throw new IllegalArgumentException("base pointer must be mapped!");
        }

        if (base.byteSize() == 0) {
            throw new IllegalArgumentException("size must greater than zero");
        }
        return asyncMadiveDirect(ioUringEventLoop, base, advise);
    }
    public static CancelableFuture<IoUringSyscallResult<Void>> asyncMadiveDirect(IoUringEventLoop ioUringEventLoop, MemorySegment base, int advise) {

        LibMman libMman = Instance.LIB_MMAN;
        long pageStart = libMman.alignPageSizeAddress(base);
        int count = (int) (base.byteSize() / LibMman.PAGE_SIZE) + 1;

        return  (CancelableFuture<IoUringSyscallResult<Void>>)
                ioUringEventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_madvise(sqe, MemorySegment.ofAddress(pageStart), (long) LibMman.PAGE_SIZE * count, advise))
                .thenApply(cqe -> new IoUringSyscallResult<>(cqe.getRes(), (Void) null));
    }

    public static CancelableFuture<IoUringSyscallResult<Void>> asyncMadive(IoUringEventLoop ioUringEventLoop, MappedByteBuffer base, int advise) {
        MemorySegment buffer = MemorySegment.ofBuffer(base);
        return asyncMadiveDirect(ioUringEventLoop, buffer, advise);
    }
}
