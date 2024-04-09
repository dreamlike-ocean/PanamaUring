package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.IoUringSyscallOwnershipResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.sync.fd.PipeFd;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipResource;

public class AsyncSplicer implements IoUringOperator {

    private final IoUringEventLoop driver;

    public AsyncSplicer(IoUringEventLoop driver) {
        this.driver = driver;
    }


    public CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> asyncReadPipe(
            OwnershipResource<PipeFd> pipeFd,
            NativeFd outFd, long outOffset,
            int nbytes, int spliceFlags
    ) {
        return (CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>>) asyncSplice(
                pipeFd.resource().readFd(), -1,
                outFd.writeFd(), outOffset,
                nbytes, spliceFlags)
                .whenComplete(pipeFd::DropWhenException)
                .thenApply(res -> new IoUringSyscallOwnershipResult<>(pipeFd, res));
    }

    public CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> asyncWritePipe(
            OwnershipResource<PipeFd> pipeFd,
            NativeFd inFd, long inOffset,
            int nbytes, int spliceFlags
    ) {
        return (CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>>) asyncSplice(
                inFd.writeFd(), inOffset,
                pipeFd.resource().writeFd(), -1,
                nbytes, spliceFlags)
                .whenComplete(pipeFd::DropWhenException)
                .thenApply(res -> new IoUringSyscallOwnershipResult<>(pipeFd, res));
    }


    private CancelableFuture<Integer> asyncSplice(
            int fdIn, long fdInOffset,
            int fdOut, long fdOutOffset,
            int len, int spliceFlags
    ) {
        return (CancelableFuture<Integer>) driver.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_splice(
                            sqe,
                            fdIn, fdInOffset,
                            fdOut, fdOutOffset,
                            len, spliceFlags
                    );
                })
                .thenApply(IoUringCqe::getRes);
    }

    public CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> asyncSendFile(
            OwnershipResource<PipeFd> midPipe,
            NativeFd outFd, long outOffset,
            NativeFd inFd, long inOffset,
            int nbytes, int spliceFlags
    ) {
        CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> writeAsyncOp = asyncWritePipe(midPipe, outFd, outOffset, nbytes, spliceFlags);
        RedirectCancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> returnValue = new RedirectCancelableFuture<>(writeAsyncOp);
        writeAsyncOp
                .whenComplete((res, t) -> {
                    int writeLen = res.result();
                    if (t != null || writeLen < 0) {
                        midPipe.drop();
                        returnValue.completeExceptionally(t);
                        return;
                    }
                    CancelableFuture<IoUringSyscallOwnershipResult<PipeFd>> writeToFdStage = asyncReadPipe(midPipe, inFd, inOffset, writeLen, spliceFlags);
                    returnValue.redirect(writeToFdStage);
                    writeToFdStage.whenComplete((res2, t2) -> {
                                if (t2 != null) {
                                    midPipe.drop();
                                    returnValue.completeExceptionally(t2);
                                    return;
                                }
                                returnValue.complete(res2);
                            });
                });
        return returnValue;
    }

    @Override
    public IoUringEventLoop owner() {
        return driver;
    }

    private static class RedirectCancelableFuture<T> extends CancelableFuture<T> {

        public RedirectCancelableFuture(CancelableFuture<T> future) {
            super(future.token());
        }

        public void redirect(CancelableFuture<T> future) {
            this.token = future.token();
        }
    }
}
