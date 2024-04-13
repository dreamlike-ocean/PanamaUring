package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.helper.NativeCallException;
import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd.addrSize;

public class AsyncMultiShotTcpServerSocketFd implements IoUringOperator, PollableFd {

    IoUringEventLoop owner;

    final int fd;

    final SocketAddress address;

    final int port;

    private volatile boolean hasListen;

    private volatile Supplier<IoUringEventLoop> subSocketEventLoopBinder = () -> owner;

    public AsyncMultiShotTcpServerSocketFd(AsyncTcpServerSocketFd fd) {
        if (!fd.hasListen) {
            throw new IllegalStateException("server socket not listen yet!");
        }
        this.owner = fd.owner;
        this.address = fd.address;
        this.port = fd.port;
        int dupCallResult = Instance.LIBC.dup(fd.fd);
        if (dupCallResult < 0) {
            throw new NativeCallException(NativeHelper.currentErrorStr());
        }
        this.fd = dupCallResult;
        this.hasListen = true;
    }

    public void setSubSocketEventLoopBinder(Supplier<IoUringEventLoop> subSocketEventLoopBinder) {
        this.subSocketEventLoopBinder = subSocketEventLoopBinder;
    }

    public CancelToken asyncMultiAccept(int flag, OwnershipMemory sockaddr, OwnershipMemory sockLen, Consumer<IoUringSyscallResult<AsyncTcpSocketFd>> callback) {
        Objects.requireNonNull(sockaddr);
        Objects.requireNonNull(sockLen);
        MemorySegment sockaddrMemory = sockaddr.resource();
        MemorySegment sockLenMemory = sockLen.resource();
        long addrSize = addrSize(address);
        if (sockaddrMemory.byteSize() != addrSize) {
            throw new IllegalArgumentException("sockaddr size must be " + addrSize);
        }
        sockLenMemory.set(ValueLayout.JAVA_INT, 0L, (int) sockaddrMemory.byteSize());


        return owner.asyncOperation(
                sqe -> fillMultiOp(sqe, flag),
                cqe -> {
                    IoUringSyscallResult<AsyncTcpSocketFd> result = null;
                    if (cqe.getRes() < 0) {
                        result = new IoUringSyscallResult<>(cqe.getRes(), null);
                    } else {
                        result = new IoUringSyscallResult<>(cqe.getRes(), parseCqe(cqe, sockaddr.resource(), sockLen.resource()));
                    }
                    callback.accept(result);
                }
        );
    }

    private AsyncTcpSocketFd parseCqe(IoUringCqe cqe, MemorySegment sockaddr, MemorySegment socklen) {
        int acceptedFd = cqe.getRes();
        int syscallRes = Instance.LIBC.getpeername(acceptedFd, sockaddr, socklen);
        if (syscallRes < 0) {
            throw new SyscallException(syscallRes);
        }
        SocketAddress remoteAddress = AsyncTcpSocketFd.inferAddress(address, sockaddr);
        IoUringEventLoop subEventLoop = subSocketEventLoopBinder.get();
        return new AsyncTcpSocketFd(subEventLoop, acceptedFd, address, remoteAddress);
    }

    private void fillMultiOp(IoUringSqe sqe, int flag) {
        Instance.LIB_URING.io_uring_prep_multishot_accept(sqe, fd, MemorySegment.NULL, MemorySegment.NULL, flag);
    }

    @Override
    public IoUringEventLoop owner() {
        return owner;
    }

    @Override
    public int fd() {
        return fd;
    }
}