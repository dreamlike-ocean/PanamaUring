package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.networking.eventloop.ReaderEventLoop;
import top.dreamlike.panama.uring.networking.stream.pipeline.IOStreamPipeline;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.util.function.LongFunction;

public final class SocketStream extends IOStream<AsyncTcpSocketFd> {
    private final AsyncTcpSocketFd socketFd;

    private boolean autoRead;

    private final IOStreamPipeline<AsyncTcpSocketFd> pipeline;

    private final IoUringEventLoop carrier;

    private LongFunction<IoUringBufferRing> choose;

    private long lastRecv;

    private CancelToken currentCurrentRecvOp = null;

    private boolean allowFallBack = false;

    private SocketStream(AsyncTcpSocketFd socketFd, boolean autoRead, boolean allowFallBack) {
        this.socketFd = socketFd;
        IoUringEventLoop ioUringEventLoop = socketFd.owner();
        this.autoRead = autoRead;
        this.pipeline = new IOStreamPipeline<>(this);
        this.carrier = ioUringEventLoop;
        if (ioUringEventLoop instanceof ReaderEventLoop readerEventLoop) {
            choose = readerEventLoop.getChoose();
        } else {
            choose = (_) -> socketFd.bufferRing();
        }
        this.lastRecv = -1;
        this.allowFallBack = allowFallBack;
        if (autoRead) {
            setAutoRead(true);
        }
    }


    @Override
    public void close() {
        socketFd.close();
    }

    @Override
    public IOStreamPipeline<AsyncTcpSocketFd> pipeline() {
        return pipeline;
    }

    @Override
    public void setAutoRead(boolean autoRead) {
        if (autoRead == this.autoRead) {
            return;
        }
        carrier.runOnEventLoop(() -> setAutoRead0(autoRead));
    }

    private void setAutoRead0(boolean autoRead) {
        if (this.autoRead == autoRead) {
            return;
        }

        boolean after = autoRead;

        if (after) {
            startRead();
            this.autoRead = true;
        } else {
            //设置为非自动读取 则判断下multiShotCancelToken是不是为空
            //若不为空则为被取消
            //cancelAutoRead会先设置为multiShotCancelToken为空但是autoRead仍为true
            if (this.currentCurrentRecvOp != null) {
                cancelAutoRead();
            }
        }
    }

    private void cancelAutoRead() {
        CancelToken token = this.currentCurrentRecvOp;
        this.currentCurrentRecvOp = null;
        cancelAutoRead0(token);
    }

    private void cancelAutoRead0(CancelToken token) {
        token.cancelOperation()
                .whenComplete((result, _) -> {
                    switch (result) {
                        case CancelToken.AlreadyResult _ -> cancelAutoRead0(token);
                        case CancelToken.SuccessResult(int _) -> this.autoRead = false;
                        case CancelToken.OtherError(int errno) -> pipeline.fireError(new SyscallException(errno));
                        case CancelToken.NoElementResult _ ->
                                pipeline.fireError(new SyscallException(-Libc.Error_H.ENOENT));
                        case CancelToken.InvalidResult _ ->
                                pipeline.fireError(new SyscallException(-Libc.Error_H.EINVAL));
                    }
                });
    }

    private void startRead() {
        IoUringBufferRing bufferRing = choose.apply(lastRecv);
        if (bufferRing == null || !bufferRing.hasAvailableElements()) {
            startPlainRead();
        } else {
            startSelectedRecv(bufferRing);
        }
    }

    private void startPlainRead() {
        var recvBuffer = Instance.LIB_JEMALLOC.mallocMemory(2048);
        this.currentCurrentRecvOp = ((CancelableFuture<BufferResult<OwnershipMemory>>) socketFd.asyncRecv(recvBuffer, (int) recvBuffer.resource().byteSize(), 0)
                .whenComplete((br, t) -> {
                    if (t != null) {
                        pipeline.fireError(t);
                        return;
                    }
                    int syscallRes = br.syscallRes();
                    if (syscallRes < 0) {
                        br.buffer().drop();
                        pipeline.fireError(new SyscallException(syscallRes));
                        return;
                    }
                    this.lastRecv = syscallRes;
                    pipeline.fireRead(br.buffer());
                    if (autoRead) {
                        startRead();
                    }
                }))
                .token();

    }

    private void startSelectedRecv(IoUringBufferRing bufferRing) {
        socketFd.bindBufferRing(bufferRing);
        //这里填什么len都行 跟着bufferRing走的
        this.currentCurrentRecvOp = ((CancelableFuture<OwnershipMemory>) socketFd.asyncRecvSelected(Integer.MAX_VALUE, 0)
                .whenComplete((ownershipMemory, t) -> {
                    if (t != null) {
                        pipeline.fireError(t);
                        if (allowFallBack) {
                            startPlainRead();
                        }
                        return;
                    }
                   pipeline.fireRead(ownershipMemory);
                }))
                .token();
    }

    @Override
    public AsyncTcpSocketFd socket() {
        return socketFd;
    }


    private static class RecvBuffer implements OwnershipMemory {
        private final OwnershipMemory originalMemory;
        private final MemorySegment canUse;

        private RecvBuffer(OwnershipMemory originalMemory, int syscallRes) {
            this.originalMemory = originalMemory;
            this.canUse = originalMemory.resource().reinterpret(syscallRes);
        }


        @Override
        public MemorySegment resource() {
            return canUse;
        }

        @Override
        public void drop() {
            originalMemory.drop();
        }
    }
}
