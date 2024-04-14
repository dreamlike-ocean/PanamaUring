package top.dreamlike.panama.uring.async.operator;

import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;

import java.util.concurrent.CompletableFuture;

public class IoUringNoOp implements IoUringOperator {

    private final IoUringEventLoop ioUringEventLoop;

    public IoUringNoOp(IoUringEventLoop ioUringEventLoop) {
        this.ioUringEventLoop = ioUringEventLoop;
    }

    public CompletableFuture<Integer> submit() {
        return ioUringEventLoop.asyncOperation(Instance.LIB_URING::io_uring_prep_nop)
                .thenApply(IoUringCqe::getRes);
    }


    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }
}
