package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpSocketFd;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.networking.eventloop.ReaderEventLoop;
import top.dreamlike.panama.uring.networking.stream.pipeline.IOStreamPipeline;

import java.util.function.LongFunction;

public final class MultiShotSocketStream extends IOStream<AsyncMultiShotTcpSocketFd> {
    private final IOStreamPipeline<AsyncMultiShotTcpSocketFd> pipeline;
    private final AsyncMultiShotTcpSocketFd socketFd;

    private CancelToken multiShotCancelToken = null;

    private LongFunction<IoUringBufferRing> choose;

    private final IoUringEventLoop carrier;

    private final IoUringEventLoop sender;

    private boolean autoRead;

    public MultiShotSocketStream(AsyncMultiShotTcpSocketFd socketFd, IoUringEventLoop sender, boolean autoRead) {
        this.socketFd = socketFd;
        IoUringEventLoop ioUringEventLoop = socketFd.owner();
        this.autoRead = autoRead;
        this.pipeline = new IOStreamPipeline<>(this);
        this.carrier = ioUringEventLoop;
        this.sender = sender;
        if (ioUringEventLoop instanceof ReaderEventLoop readerEventLoop) {
            choose = readerEventLoop.getChoose();
        } else {
            final IoUringBufferRing bufferRing = PanamaUringSecret.findBufferRing.apply(socketFd);
            choose = (_) -> bufferRing;
        }

        if (autoRead) {
            startAutoRead();
        }
    }

    public MultiShotSocketStream(AsyncMultiShotTcpSocketFd socketFd, boolean autoRead) {
        this(socketFd, socketFd.owner(), autoRead);
    }

    @Override
    public void setAutoRead(boolean autoRead) {
        if (autoRead == this.autoRead) {
            return;
        }
        carrier.runOnEventLoop(() -> setAutoRead0(autoRead));
    }

    @Override
    public AsyncMultiShotTcpSocketFd socket() {
        return socketFd;
    }

    private void startAutoRead() {
        IoUringBufferRing bufferRing = choose.apply(-1);
        this.multiShotCancelToken = socketFd.asyncRecvMulti(0, bufferRing, ioUringCall -> {
            if (ioUringCall.canceled()) {
                return;
            }
            if (ioUringCall.res() < 0) {
                this.autoRead = false;
                pipeline.fireError(new SyscallException(ioUringCall.res()));
                return;
            }
            pipeline.fireRead(ioUringCall.value());
        });
    }


    private void setAutoRead0(boolean autoRead) {
        if (this.autoRead == autoRead) {
            return;
        }

        boolean after = autoRead;

        if (after) {
            startAutoRead();
            this.autoRead = true;
        } else {
            //设置为非自动读取 则判断下multiShotCancelToken是不是为空
            //若不为空则为被取消
            //cancelAutoRead会先设置为multiShotCancelToken为空但是autoRead仍为true
            if (this.multiShotCancelToken != null) {
                cancelAutoRead();
            }
        }
    }

    /**
     * 尽可能取消。。
     */
    private void cancelAutoRead() {
        CancelToken token = this.multiShotCancelToken;
        this.multiShotCancelToken = null;
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

    @Override
    public void close() {
        socketFd.close();
    }

    @Override
    public IOStreamPipeline<AsyncMultiShotTcpSocketFd> pipeline() {
        return pipeline;
    }

}
