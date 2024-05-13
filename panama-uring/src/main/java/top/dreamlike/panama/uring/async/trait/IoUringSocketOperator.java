package top.dreamlike.panama.uring.async.trait;

import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

public interface IoUringSocketOperator extends IoUringOperator, NativeFd {
    default CancelableFuture<BufferResult<OwnershipMemory>> asyncSend(OwnershipMemory buffer, int len, int flag) {
        return ((CancelableFuture<BufferResult<OwnershipMemory>>) owner().asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_send(sqe, fd(), buffer.resource(), len, flag))
                .whenComplete(buffer::DropWhenException)
                .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes())));
    }


    default CancelableFuture<BufferResult<OwnershipMemory>> asyncSendZc(OwnershipMemory buffer, int len, int flag, int zcFlags) {
        class ZCContext {
            ZCContext.Stage stage = ZCContext.Stage.WAIT_MORE;
            int sendRes;

            private enum Stage {
                WAIT_MORE,
                MORE,
                END;
            }
        }
        ZCContext context = new ZCContext();
        return (CancelableFuture<BufferResult<OwnershipMemory>>) (new CancelableFuture<BufferResult<OwnershipMemory>>(promise ->
                owner().asyncOperation(
                        sqe -> Instance.LIB_URING.io_uring_prep_send_zc(sqe, fd(), buffer.resource(), len, flag, zcFlags),
                        cqe -> {
                            if (cqe.hasMore()) {
                                context.stage = ZCContext.Stage.MORE;
                                context.sendRes = cqe.getRes();
                            } else {
                                context.stage = ZCContext.Stage.END;
                                promise.complete(new BufferResult<>(buffer, cqe.getRes()));
                            }
                        }
                )
        ).whenComplete(buffer::DropWhenException));


    }

    default public CancelableFuture<BufferResult<OwnershipMemory>> asyncRecv(OwnershipMemory buffer, int len, int flag) {
        return (CancelableFuture<BufferResult<OwnershipMemory>>)
                owner().asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_recv(sqe, fd(), buffer.resource(), len, flag))
                        .whenComplete(buffer::DropWhenException)
                        .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()));
    }


}
