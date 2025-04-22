package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface IoUringSelectedReadableFd extends IoUringOperator, NativeFd {
    IoUringBufferRing bufferRing();

    default CancelableFuture<OwnershipMemory> asyncSelectedRead(int len, int offset) {
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<OwnershipMemory>) owner().asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    bufferRing.fillSqe(sqe);
                })
                .thenComposeAsync(cqe -> {
                    int syscallResult = cqe.getRes();
                    if (syscallResult < 0) {
                        return CompletableFuture.failedFuture(new SyscallException(syscallResult));
                    } else {
                        int bid = cqe.getBid();
                        var ringElement = bufferRing.removeBuffer(bid);
                        return CompletableFuture.completedFuture(ringElement.slice(0, syscallResult));
                    }
                }, r -> owner().runOnEventLoop(r));
    }

    default CancelableFuture<IoUringSyscallResult<OwnershipMemory>> asyncSelectedReadResult(int len, int offset) {
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<IoUringSyscallResult<OwnershipMemory>>) owner().asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    bufferRing.fillSqe(sqe);
                })
                .thenApplyAsync(cqe -> {
                    //强制制定在eventloop上 防止外部get导致切换线程
                    IoUringSyscallResult<OwnershipMemory> result;
                    if (cqe.getRes() < 0) {
                        result = new IoUringSyscallResult<>(cqe.getRes(), OwnershipMemory.of(MemorySegment.NULL));
                    } else {
                        int readLen = cqe.getRes();
                        int bid = cqe.getBid();
                        var ringElement = bufferRing.removeBuffer(bid);
                        result = new IoUringSyscallResult<>(cqe.getRes(), ringElement.slice(0, readLen));
                    }
                    return result;
                }, r -> owner().runOnEventLoop(r));

    }

}