package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

public class AsyncMultiShotTcpSocketFd implements IoUringOperator {
    final int fd;

    final SocketAddress localAddress;

    final SocketAddress remoteAddress;

    final IoUringEventLoop ioUringEventLoop;

    final IoUringBufferRing bufferRing;

    public AsyncMultiShotTcpSocketFd(AsyncTcpSocketFd fd, IoUringBufferRing ring) {
        if (!fd.hasConnected) {
            throw new IllegalStateException("socket not connected yet!");

        }
        Objects.requireNonNull(ring);
        this.fd = Instance.LIBC.dup(fd.fd);
        this.localAddress = fd.localAddress;
        this.remoteAddress = fd.remoteAddress;
        this.ioUringEventLoop = fd.owner();
        this.bufferRing = ring;
    }


    public AsyncMultiShotTcpSocketFd(AsyncTcpSocketFd fd) {
        this(fd, fd.bufferRing);
    }

    public CancelToken asyncRecvMulti(Consumer<IoUringSyscallResult<OwnershipMemory>> handleRecv) {
        return asyncRecvMulti( 0, handleRecv);
    }

    public CancelToken asyncRecvMulti( int flag, Consumer<IoUringSyscallResult<OwnershipMemory>> handleRecv) {
        return ioUringEventLoop.asyncOperation(
                sqe -> fillMultiOp(sqe, flag),
                cqe -> {
                    IoUringSyscallResult<OwnershipMemory> result = null;
                    if (cqe.getRes() < 0) {
                        result = new IoUringSyscallResult<>(cqe.getRes(), null);
                    } else {
                        result = new IoUringSyscallResult<>(cqe.getRes(), parseCqe(cqe));
                    }
                    handleRecv.accept(result);
                }
        );
    }

    private OwnershipMemory parseCqe(IoUringCqe cqe) {
        IoUringBufferRingElement memoryByBid = bufferRing.removeBuffer(cqe.getBid()).resultNow();
        return new OwnershipBufferRingElement(memoryByBid, cqe.getRes());
    }

    private void fillMultiOp(IoUringSqe sqe, int flag) {
        Instance.LIB_URING.io_uring_prep_recv_multishot(sqe, fd, flag, bufferRing.getBufferGroupId());
    }

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }
}
