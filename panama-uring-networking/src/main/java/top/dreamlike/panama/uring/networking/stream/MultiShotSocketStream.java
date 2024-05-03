package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpSocketFd;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.networking.eventloop.ReaderEventLoop;
import top.dreamlike.panama.uring.networking.stream.pipeline.IOStreamPipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongFunction;

public final class MultiShotSocketStream extends IOStream<AsyncMultiShotTcpSocketFd> {
    private final IOStreamPipeline<AsyncMultiShotTcpSocketFd> pipeline;
    private final AsyncMultiShotTcpSocketFd socketFd;

    private CancelToken multiShotCancelToken = null;


    private AtomicBoolean registered = new AtomicBoolean(false);

    private LongFunction<IoUringBufferRing> choose;

    private final IoUringEventLoop carrier;

    private boolean autoRead;

    private MultiShotSocketStream(AsyncMultiShotTcpSocketFd socketFd, boolean autoRead) {
        this.socketFd = socketFd;
        IoUringEventLoop ioUringEventLoop = socketFd.owner();
        this.autoRead = autoRead;
        this.pipeline = new IOStreamPipeline<>(ioUringEventLoop, this);
        this.carrier = ioUringEventLoop;
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


    private void startAutoRead() {
        IoUringBufferRing bufferRing = choose.apply(-1);
        this.multiShotCancelToken = socketFd.asyncRecvMulti(0, bufferRing, ioUringCall -> {
            if (ioUringCall.canceled()) {
                return;
            }
            if (ioUringCall.res() < 0) {
                pipeline.fireError(new SyscallException(ioUringCall.res()));
                return;
            }
            pipeline.fireRead(ioUringCall.value());
        });
    }


    @Override
    public void setAutoRead(boolean autoRead) {
        if (autoRead == this.autoRead) {
            return;
        }
        carrier.runOnEventLoop(() -> setAutoRead0(autoRead));
    }

    @Override
    public void startWrite(Object msg, CompletableFuture<Integer> promise) {
        pipeline.fireWrite(msg, promise);
    }

    @Override
    public AsyncMultiShotTcpSocketFd fd() {
        return socketFd;
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

    private void cancelAutoRead0( CancelToken token) {
        this.multiShotCancelToken = null;
        token.cancelOperation()
                .whenComplete((result, _) -> {

                    if (result instanceof CancelToken.SuccessResult successResult) {
                        this.autoRead = false;
                        return;
                    }
                    if (result instanceof CancelToken.AlreadyResult) {
                        cancelAutoRead0(token);
                        return;
                    }
                    if (result instanceof CancelToken.OtherError otherError) {
                        pipeline.fireError(new SyscallException(otherError.errno()));
                        return;
                    }
                });
    }

    @Override
    public void close() {
        socketFd.close();
    }

    @Override
    public IOStreamPipeline pipeline() {
        return pipeline;
    }

}
