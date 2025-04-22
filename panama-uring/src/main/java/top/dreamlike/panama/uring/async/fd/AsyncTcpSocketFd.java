package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.async.trait.IoUringSocketOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.CloseHandle;
import top.dreamlike.panama.uring.helper.LambdaHelper;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn6;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrUn;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.panama.uring.nativelib.Instance.LIBC;

public class AsyncTcpSocketFd implements IoUringAsyncFd, IoUringSelectedReadableFd, IoUringSocketOperator {

    private static final VarHandle BUFFER_RING_VH;

    static {
        try {
            BUFFER_RING_VH = MethodHandles.lookup().findVarHandle(AsyncTcpSocketFd.class, "bufferRing", IoUringBufferRing.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final IoUringEventLoop ioUringEventLoop;
    final int fd;
    final MemoryAllocator allocator;
    private final CloseHandle closeHandle;
    SocketAddress localAddress;
    SocketAddress remoteAddress;
    volatile boolean hasConnected;
    IoUringBufferRing bufferRing;

    AsyncTcpSocketFd(IoUringEventLoop ioUringEventLoop, int fd, SocketAddress localAddress, SocketAddress remoteAddress) {
        this.ioUringEventLoop = ioUringEventLoop;
        this.allocator = ioUringEventLoop.getMemoryAllocator();
        this.fd = fd;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.hasConnected = true;
        this.closeHandle = new CloseHandle(IoUringSocketOperator.super::close);
    }

    public AsyncTcpSocketFd(IoUringEventLoop ioUringEventLoop, SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.fd = socketSysCall(remoteAddress, ioUringEventLoop.getMemoryAllocator());
        this.ioUringEventLoop = ioUringEventLoop;
        this.allocator = ioUringEventLoop.getMemoryAllocator();
        this.closeHandle = new CloseHandle(IoUringSocketOperator.super::close);
    }

    static SocketAddress inferAddress(SocketAddress peerAddress, MemorySegment sockaddrMemory) {
        return switch (peerAddress) {
            case UnixDomainSocketAddress _ -> peerAddress;
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> parseV4(sockaddrMemory);
                case Inet6Address _ -> parseV6(sockaddrMemory);
                default -> throw new IllegalStateException("Unexpected value: " + inetSocketAddress.getAddress());
            };
            default -> throw new IllegalStateException("Unexpected value: " + peerAddress);
        };
    }

    static long addrSize(SocketAddress address) {
        return switch (address) {
            case UnixDomainSocketAddress _ -> SocketAddrUn.LAYOUT.byteSize();
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> SocketAddrIn.LAYOUT.byteSize();
                case Inet6Address _ -> SocketAddrIn6.LAYOUT.byteSize();
                default -> throw new IllegalStateException("Unexpected value: " + inetSocketAddress.getAddress());
            };
            default -> throw new IllegalStateException("Unexpected value: " + address);
        };
    }

    static SocketAddress parseV6(MemorySegment sockaddrMemory) {
        SocketAddrIn6 socketAddrIn6 = Instance.STRUCT_PROXY_GENERATOR.enhance(sockaddrMemory);
        byte[] address = new byte[16];
        MemorySegment.copy(MemorySegment.ofArray(address), 0, sockaddrMemory, SocketAddrIn6.SIN6_ADDR_OFFSET, 16);
        try {
            return new InetSocketAddress(Inet6Address.getByAddress(address), Short.toUnsignedInt(NativeHelper.ntohs(socketAddrIn6.getSin6_port())));
        } catch (UnknownHostException unknownHostException) {
            //不应该在这里抛出异常
            throw new IllegalArgumentException(unknownHostException);
        }
    }

    static SocketAddress parseV4(MemorySegment sockaddrMemory) {
        SocketAddrIn socketAddrIn = Instance.STRUCT_PROXY_GENERATOR.enhance(sockaddrMemory);
        byte[] address = new byte[4];
        int addr = NativeHelper.ntohl(socketAddrIn.getSin_addr());
        address[0] = (byte) ((addr >> 24) & 0xFF);
        address[1] = (byte) ((addr >> 16) & 0xFF);
        address[2] = (byte) ((addr >> 8) & 0xFF);
        address[3] = (byte) (addr & 0xFF);
        try {
            return new InetSocketAddress(Inet4Address.getByAddress(address), Short.toUnsignedInt(NativeHelper.ntohs(socketAddrIn.getSin_port())));
        } catch (UnknownHostException unknownHostException) {
            //不应该在这里抛出异常
            throw new IllegalArgumentException(unknownHostException);
        }
    }

    static OwnershipMemory mallocAddr(SocketAddress address, MemoryAllocator memoryAllocator) {
        return NativeHelper.mallocAddr(address, memoryAllocator);
    }

    static int socketSysCall(SocketAddress address, MemoryAllocator memoryAllocator) {
        return NativeHelper.socketSysCall(address, memoryAllocator);
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }

    @Override
    public int fd() {
        return fd;
    }

    public CancelableFuture<Integer> asyncConnect() {
        if (hasConnected) {
            throw new IllegalStateException("already connected.");
        }
        OwnershipMemory addr = mallocAddr(remoteAddress, allocator);
        return ((CancelableFuture<Integer>) (owner()
                .asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_connect(sqe, fd, addr.resource(), (int) addr.resource().byteSize()))
                .whenComplete((_, _) -> addr.drop())
                .thenApply(IoUringCqe::getRes))
                .whenComplete((res, t) -> {
                    if (res >= 0 && t == null) {
                        hasConnected = true;
                        localAddress = getSocketName();
                    }
                })
        );
    }

    private SocketAddress getSocketName() {
        if (remoteAddress instanceof UnixDomainSocketAddress) {
            return localAddress;
        }
        long addrSize = addrSize(remoteAddress);
        long totalSize = addrSize + ValueLayout.JAVA_INT.byteSize();
        try (OwnershipMemory sockaddrMemory = allocator.allocateOwnerShipMemory(totalSize)) {
            MemorySegment resource = sockaddrMemory.resource();
            MemorySegment sockAddrMemory = resource.asSlice(0, addrSize);
            MemorySegment sockLenMemory = resource.asSlice(addrSize, ValueLayout.JAVA_INT.byteSize());
            sockLenMemory.set(ValueLayout.JAVA_INT, 0L, (int) sockAddrMemory.byteSize());
            int i = LIBC.getsockname(fd, sockAddrMemory, sockLenMemory);
            if (i < 0) {
                throw new IllegalArgumentException("getsockname error, reason:" + NativeHelper.currentErrorStr());
            }
            return inferAddress(remoteAddress, sockAddrMemory);
        } catch (Exception t) {
            throw new IllegalStateException(t);
        }
    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncRecv(OwnershipMemory buffer, int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before recv, must connect first.");
        }
        return IoUringSocketOperator.super.asyncRecv(buffer, len, flag);
    }

    public CancelableFuture<OwnershipMemory> asyncRecvSelected(int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before recv, must connect first.");
        }
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<OwnershipMemory>) owner().asyncOperation(sqe -> {
            Instance.LIB_URING.io_uring_prep_recv(sqe, fd, MemorySegment.NULL, len, flag);
            bufferRing.fillSqe(sqe);
        }).thenCompose(cqe -> {
            int syscallResult = cqe.getRes();
            if (syscallResult < 0) {
                return CompletableFuture.failedFuture(new SyscallException(syscallResult));
            } else {
                int bid = cqe.getBid();
                var ringElement = bufferRing.removeBuffer(bid);
                return CompletableFuture.completedFuture(ringElement.slice(0, syscallResult));
            }
        });

    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncSend(OwnershipMemory buffer, int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before send, must connect first.");
        }
        return IoUringSocketOperator.super.asyncSend(buffer, len, flag);
    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncSendZc(OwnershipMemory buffer, int len, int flag, int zcFlags) {
        if (!hasConnected) {
            throw new IllegalStateException("before send, must connect first.");
        }
        return IoUringSocketOperator.super.asyncSendZc(buffer, len, flag, zcFlags);
    }

    @Override
    public void close() {
        closeHandle.close();
    }

    public boolean bindBufferRing(IoUringBufferRing bufferRing) {
        BUFFER_RING_VH.setVolatile(this, bufferRing);
        return true;
    }

    @Override
    public String toString() {
        return "AsyncTcpSocket{" +
               "ioUringEventLoop=" + ioUringEventLoop +
               ", fd=" + fd +
               ", localAddress=" + localAddress +
               ", remoteAddress=" + remoteAddress +
               '}';
    }

    @Override
    public IoUringBufferRing bufferRing() {
        return bufferRing;
    }
}