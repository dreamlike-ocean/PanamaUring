package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Supplier;

public class AsyncTcpServerSocketFd implements IoUringAsyncFd {

    private final static Libc LIBC = Instance.LIBC;

    private IoUringEventLoop owner;

    private final int fd;

    private final SocketAddress address;

    private final int port;

    private volatile boolean hasListen;

    private volatile Supplier<IoUringEventLoop> subSocketEventLoopBinder = () -> owner;

    public AsyncTcpServerSocketFd(IoUringEventLoop owner, SocketAddress address, int port) {
        this.owner = owner;
        this.address = address;
        this.port = port;
        this.fd = AsyncTcpSocketFd.socketSysCall(address);
        this.hasListen = false;
    }

    public int bind() {
        OwnershipMemory addr = AsyncTcpSocketFd.mallocAddr(address);
        try (addr) {
            int listenRes = LIBC.bind(fd, addr.resource(), (int) addr.resource().byteSize());
            if (listenRes < 0) {
                throw new IllegalArgumentException(STR."bind error, reason: \{DebugHelper.currentErrorStr()}");
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
                throw new IllegalArgumentException(STR."listen error, reason\{DebugHelper.currentErrorStr()}");
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
            throw new IllegalArgumentException(STR."sockaddr size must be \{addrSize()}");
        }
        sockLenMemory.set(ValueLayout.JAVA_INT, 0L, (int) sockaddrMemory.byteSize());

        return (CancelableFuture<AsyncTcpSocketFd>) owner.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_accept(sqe, fd, sockaddrMemory, sockLenMemory, flag))
                .thenApply(cqe -> {
                    try (sockaddr; sockLen) {
                        int acceptFd = cqe.getRes();
                        if (acceptFd < 0) {
                            throw new IllegalArgumentException(STR."accept fail, reason: \{DebugHelper.getErrorStr(-acceptFd)}");
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
    public CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>> asyncWriteV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        return IoUringAsyncFd.super.asyncWriteV(iovec, nr_vecs, offset);
    }

    @Override
    public CancelableFuture<BufferResult<OwnershipMemory>> asyncWrite(OwnershipMemory buffer, int len, int offset) {
        return IoUringAsyncFd.super.asyncWrite(buffer, len, offset);
    }

    @Override
    public CancelableFuture<BufferResult<OwnershipResource<NativeArrayPointer<Iovec>>>> asyncReadV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<IoUringCqe> asyncSelectedRead(int len, int offset, short bufferGroupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<BufferResult<OwnershipMemory>> asyncRead(OwnershipMemory buffer, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int write(MemorySegment buf, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(MemorySegment buf, int count) {
        throw new UnsupportedOperationException();
    }
}
