package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Supplier;

public class AsyncTcpServerSocketFd implements IoUringAsyncFd, PollableFd {

    private final static Libc LIBC = Instance.LIBC;

    IoUringEventLoop owner;

    final int fd;

    final SocketAddress address;

    final int port;

    volatile boolean hasListen;

    private volatile Supplier<IoUringEventLoop> subSocketEventLoopBinder = () -> owner;

    public AsyncTcpServerSocketFd(IoUringEventLoop owner, SocketAddress address, int port) {
        this.owner = owner;
        this.address = address;
        this.port = port;
        this.fd = AsyncTcpSocketFd.socketSysCall(address, owner.getMemoryAllocator());
        this.hasListen = false;
    }

    public int bind() {
        OwnershipMemory addr = AsyncTcpSocketFd.mallocAddr(address, owner.getMemoryAllocator());
        try (addr) {
            int listenRes = LIBC.bind(fd, addr.resource(), (int) addr.resource().byteSize());
            if (listenRes < 0) {
                throw new IllegalArgumentException("bind error, reason: " + NativeHelper.currentErrorStr());
            }
            return listenRes;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int listen(int backlog) {
        if (hasListen) {
            return 0;
        }
        synchronized (this) {
            if (hasListen) {
                return 0;
            }
            int listenRes = LIBC.listen(fd, backlog);
            if (listenRes < 0) {
                throw new IllegalArgumentException("listen error, reason: " + NativeHelper.currentErrorStr());
            }
            hasListen = true;
        }
        return 0;
    }

    public long addrSize() {
        return AsyncTcpSocketFd.addrSize(address);
    }

    public CancelableFuture<AsyncTcpSocketFd> asyncAccept(int flag, OwnershipMemory sockaddr, OwnershipMemory sockLen) {
        if (!hasListen) {
            throw new IllegalStateException("server not listen");
        }
        Objects.requireNonNull(sockaddr);
        Objects.requireNonNull(sockLen);
        MemorySegment sockaddrMemory = sockaddr.resource();
        MemorySegment sockLenMemory = sockLen.resource();
        if (sockaddrMemory.byteSize() != addrSize()) {
            throw new IllegalArgumentException("sockaddr size must be " + addrSize());
        }
        sockLenMemory.set(ValueLayout.JAVA_INT, 0L, (int) sockaddrMemory.byteSize());

        return (CancelableFuture<AsyncTcpSocketFd>) owner.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_accept(sqe, fd, sockaddrMemory, sockLenMemory, flag))
                .thenApply(cqe -> {
                    try (sockaddr; sockLen) {
                        int acceptFd = cqe.getRes();
                        if (acceptFd < 0) {
                            throw new SyscallException(acceptFd);
                        }
                        SocketAddress remoteAddress = AsyncTcpSocketFd.inferAddress(address, sockaddrMemory);
                        IoUringEventLoop subEventLoop = subSocketEventLoopBinder.get();
                        return new AsyncTcpSocketFd(subEventLoop, acceptFd, address, remoteAddress);
                    } catch (Exception exception) {
                        //不应该在这里抛出异常 无视就行了
                        throw new IllegalArgumentException(exception);
                    }
                });
    }


    public void setSubSocketEventLoopBinder(Supplier<IoUringEventLoop> subSocketEventLoopBinder) {
        Objects.requireNonNull(subSocketEventLoopBinder, "subSocketEventLoopBinder can not be null");
        this.subSocketEventLoopBinder = subSocketEventLoopBinder;
    }

    @Override
    public IoUringEventLoop owner() {
        return owner;
    }

    @Override
    public int fd() {
        return fd;
    }

    @Override
    public int readFd() {
        throw new UnsupportedOperationException("server socket fd is not readable");
    }

    @Override
    public int writeFd() {
        throw new UnsupportedOperationException("server socket fd is not writable");
    }
}
