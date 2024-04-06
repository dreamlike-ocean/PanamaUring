package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.util.concurrent.CompletableFuture;

public record AsyncFileFd(IoUringEventLoop ioUringEventLoop, int fd) implements IoUringAsyncFd {

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }

    public CancelableFuture<Integer> asyncFsync(int fsync_flags) {
        return (CancelableFuture<Integer>) ioUringEventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_fsync(sqe, fd, fsync_flags))
                .thenApply(IoUringCqe::getRes);
    }

    public CancelableFuture<Integer> asyncFsync(int fsync_flags, int offset, int len) {
        return (CancelableFuture<Integer>) ioUringEventLoop.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_fsync(sqe, fd, fsync_flags);
                    sqe.setLen(len);
                    sqe.setOffset(offset);
                })
                .thenApply(IoUringCqe::getRes);
    }

    public static CancelableFuture<AsyncFileFd> asyncOpen(IoUringEventLoop ioUringEventLoop, OwnershipMemory path, int flags) {
        return (CancelableFuture<AsyncFileFd>) ioUringEventLoop.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_openat(sqe, -1, path.resource(), flags, 0))
                .whenComplete((_, _) -> path.drop())
                .thenCompose(cqe -> cqe.getRes() <= 0 ? CompletableFuture.failedFuture(new IllegalArgumentException("open file Fail! reason: " + DebugHelper.getErrorStr(-cqe.getRes()))) : CompletableFuture.completedFuture(cqe.getRes()))
                .thenApply(fd -> new AsyncFileFd(ioUringEventLoop, fd));
    }
}
