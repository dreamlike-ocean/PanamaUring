package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.async.trait.IoUringSocketOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.CloseHandle;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

public class AsyncMultiShotTcpSocketFd implements IoUringSocketOperator {
    final int fd;

    final SocketAddress localAddress;

    final SocketAddress remoteAddress;

    final IoUringEventLoop ioUringEventLoop;

    final IoUringBufferRing bufferRing;

    final CloseHandle closeHandle;

    public AsyncMultiShotTcpSocketFd(AsyncTcpSocketFd fd, IoUringBufferRing ring) {
        if (!fd.hasConnected) {
            throw new IllegalStateException("socket not connected yet!");
        }
        Objects.requireNonNull(ring);
        this.fd = fd.fd;
        this.localAddress = fd.localAddress;
        this.remoteAddress = fd.remoteAddress;
        this.ioUringEventLoop = fd.owner();
        this.bufferRing = ring;
        this.closeHandle = new CloseHandle(() -> Instance.LIBC.close(this.fd));
    }


    public static CancelableFuture<AsyncMultiShotTcpSocketFd> connect(IoUringEventLoop ioUringEventLoop, SocketAddress remoteAddress, IoUringBufferRing bufferRing) {
        Objects.requireNonNull(bufferRing);
        Objects.requireNonNull(ioUringEventLoop);
        Objects.requireNonNull(remoteAddress);

        AsyncTcpSocketFd tcpSocketFd = new AsyncTcpSocketFd(ioUringEventLoop, remoteAddress);
        return (CancelableFuture<AsyncMultiShotTcpSocketFd>) tcpSocketFd.asyncConnect()
                .thenComposeAsync(syscall -> {
                    if (syscall < 0) {
                        return CancelableFuture.failedFuture(new SyscallException(syscall));
                    }
                    return CancelableFuture.completedFuture(new AsyncMultiShotTcpSocketFd(tcpSocketFd));
                });
    }


    public AsyncMultiShotTcpSocketFd(AsyncTcpSocketFd fd) {
        this(fd, fd.bufferRing);
    }

    public CancelToken asyncRecvMulti(Consumer<IoUringSyscallResult<OwnershipMemory>> handleRecv) {
        return asyncRecvMulti(0, handleRecv);
    }

    public CancelToken asyncRecvMulti(int flag, Consumer<IoUringSyscallResult<OwnershipMemory>> handleRecv) {
        return asyncRecvMulti(flag, this.bufferRing, handleRecv);
    }

    public CancelToken asyncRecvMulti(int flag, IoUringBufferRing bufferRing, Consumer<IoUringSyscallResult<OwnershipMemory>> handleRecv) {
        return ioUringEventLoop.asyncOperation(
                sqe -> fillMultiOp(sqe, flag, bufferRing),
                cqe -> {
                    IoUringSyscallResult<OwnershipMemory> result;
                    if (cqe.getRes() < 0) {
                        result = new IoUringSyscallResult<>(cqe.getRes(), OwnershipMemory.of(MemorySegment.NULL));
                    } else {
                        result = new IoUringSyscallResult<>(cqe.getRes(), parseCqe(cqe));
                    }
                    handleRecv.accept(result);
                }
        );
    }

    private OwnershipMemory parseCqe(IoUringCqe cqe) {
        return bufferRing.removeBuffer(cqe.getBid()).resultNow();
    }

    private void fillMultiOp(IoUringSqe sqe, int flag, IoUringBufferRing ring) {
        Instance.LIB_URING.io_uring_prep_recv_multishot(sqe, fd, flag);
        ring.fillSqe(sqe);
    }

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }

    static {
        PanamaUringSecret.findBufferRing = (fd) -> fd.bufferRing;
    }

    @Override
    public int fd() {
        return fd;
    }

    public void close() {
        closeHandle.close();
    }
}
