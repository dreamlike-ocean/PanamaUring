package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn6;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrUn;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.panama.uring.nativelib.Instance.LIBC;
import static top.dreamlike.panama.uring.nativelib.libs.Libc.Socket_H.OptName.SO_REUSEADDR;
import static top.dreamlike.panama.uring.nativelib.libs.Libc.Socket_H.SetSockOpt.SOL_SOCKET;

public class AsyncTcpSocketFd implements IoUringAsyncFd, PollableFd, IoUringSelectedReadableFd {

    private static final VarHandle BUFFER_RING_VH;

    private final IoUringEventLoop ioUringEventLoop;

    private final int fd;

    private SocketAddress localAddress;

    private SocketAddress remoteAddress;

    private volatile boolean hasConnected;

    private IoUringBufferRing bufferRing;

    AsyncTcpSocketFd(IoUringEventLoop ioUringEventLoop, int fd, SocketAddress localAddress, SocketAddress remoteAddress) {
        this.ioUringEventLoop = ioUringEventLoop;
        this.fd = fd;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.hasConnected = true;
    }

    public AsyncTcpSocketFd(IoUringEventLoop ioUringEventLoop, SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.fd = socketSysCall(remoteAddress);
        this.ioUringEventLoop = ioUringEventLoop;
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
        OwnershipMemory addr = mallocAddr(remoteAddress);
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
        try (OwnershipMemory sockaddrMemory = Instance.LIB_JEMALLOC.mallocMemory(totalSize)) {
            MemorySegment resource = sockaddrMemory.resource();
            MemorySegment sockAddrMemory = resource.asSlice(0, addrSize);
            MemorySegment sockLenMemory = resource.asSlice(addrSize, ValueLayout.JAVA_INT.byteSize());
            sockLenMemory.set(ValueLayout.JAVA_INT, 0L, (int) sockAddrMemory.byteSize());
            int i = LIBC.getsockname(fd, sockAddrMemory, sockLenMemory);
            if (i < 0) {
                throw new IllegalArgumentException("getsockname error, reason:" + DebugHelper.currentErrorStr());
            }
            return inferAddress(remoteAddress, sockAddrMemory);
        } catch (Exception t) {
            throw new IllegalStateException(t);
        }
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
            return new InetSocketAddress(Inet6Address.getByAddress(address), Short.toUnsignedInt(DebugHelper.ntohs(socketAddrIn6.getSin6_port())));
        } catch (UnknownHostException unknownHostException) {
            //不应该在这里抛出异常
            throw new IllegalArgumentException(unknownHostException);
        }
    }

    static SocketAddress parseV4(MemorySegment sockaddrMemory) {
        SocketAddrIn socketAddrIn = Instance.STRUCT_PROXY_GENERATOR.enhance(sockaddrMemory);
        byte[] address = new byte[4];
        int addr = DebugHelper.ntohl(socketAddrIn.getSin_addr());
        address[0] = (byte) ((addr >> 24) & 0xFF);
        address[1] = (byte) ((addr >> 16) & 0xFF);
        address[2] = (byte) ((addr >> 8) & 0xFF);
        address[3] = (byte) (addr & 0xFF);
        try {
            return new InetSocketAddress(Inet4Address.getByAddress(address), Short.toUnsignedInt(DebugHelper.ntohs(socketAddrIn.getSin_port())));
        } catch (UnknownHostException unknownHostException) {
            //不应该在这里抛出异常
            throw new IllegalArgumentException(unknownHostException);
        }
    }

    static OwnershipMemory mallocAddr(SocketAddress address) {
        return switch (address) {
            case UnixDomainSocketAddress udAddress -> udsAddress(udAddress);
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address v4 -> ipv4Address(v4, inetSocketAddress.getPort());
                case Inet6Address v6 -> ipv6Address(v6, inetSocketAddress.getPort());
                default -> throw new IllegalStateException("Unexpected value: " + inetSocketAddress.getAddress());
            };
            default -> throw new IllegalStateException("Unexpected value: " + address);
        };
    }

    static OwnershipMemory ipv4Address(Inet4Address inet4Address, int port) {
        MemoryLayout memoryLayout = Instance.STRUCT_PROXY_GENERATOR.extract(SocketAddrIn.class);
        OwnershipMemory sockaddr_inMemory = Instance.LIB_JEMALLOC.mallocMemory(memoryLayout.byteSize());
        SocketAddrIn socketAddrIn = Instance.STRUCT_PROXY_GENERATOR.enhance(sockaddr_inMemory.resource());
        socketAddrIn.setSin_family((short) Libc.Socket_H.Domain.AF_INET);
        socketAddrIn.setSin_port(DebugHelper.htons((short) port));
        byte[] addr = inet4Address.getAddress();
        int address = addr[3] & 0xFF;
        address |= ((addr[2] << 8) & 0xFF00);
        address |= ((addr[1] << 16) & 0xFF0000);
        address |= ((addr[0] << 24) & 0xFF000000);
        socketAddrIn.setSin_addr(DebugHelper.htonl(address));
        return sockaddr_inMemory;
    }

    static OwnershipMemory udsAddress(UnixDomainSocketAddress unixDomainSocketAddress) {
        String pathName = unixDomainSocketAddress.getPath().toAbsolutePath().toString();
        if (pathName.length() > 107) {
            throw new IllegalArgumentException("path length must less than 107");
        }
        OwnershipMemory socketAddrUnMemory = Instance.LIB_JEMALLOC.mallocMemory(SocketAddrUn.LAYOUT.byteSize());
        SocketAddrUn socketAddrUn = Instance.STRUCT_PROXY_GENERATOR.enhance(socketAddrUnMemory.resource());
        socketAddrUn.setSun_family((short) Libc.Socket_H.Domain.AF_UNIX);
        socketAddrUnMemory.resource().setString(SocketAddrUn.SUN_PATH_OFFSET, pathName);
        return socketAddrUnMemory;
    }

    static OwnershipMemory ipv6Address(Inet6Address inet6Address, int port) {
        MemoryLayout memoryLayout = Instance.STRUCT_PROXY_GENERATOR.extract(SocketAddrIn6.class);
        byte[] addressAddress = inet6Address.getAddress();
        OwnershipMemory sockaddr_in6Memory = Instance.LIB_JEMALLOC.mallocMemory(memoryLayout.byteSize());
        SocketAddrIn6 socketAddrIn6 = Instance.STRUCT_PROXY_GENERATOR.enhance(sockaddr_in6Memory.resource());
        socketAddrIn6.setSin6_family((short) Libc.Socket_H.Domain.AF_INET6);
        socketAddrIn6.setSin6_port(DebugHelper.htons((short) port));
        socketAddrIn6.setSin_flowinfo(0);
        MemorySegment.copy(sockaddr_in6Memory.resource(), SocketAddrIn6.SIN6_ADDR_OFFSET, MemorySegment.ofArray(addressAddress), 0, addressAddress.length);
        return sockaddr_in6Memory;
    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncRecv(OwnershipMemory buffer, int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before recv, must connect first.");
        }
        return (CancelableFuture<BufferResult<OwnershipMemory>>)
                owner().asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_recv(sqe, fd, buffer.resource(), len, flag))
                        .whenComplete(buffer::DropWhenException)
                        .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes()));
    }

    public CancelableFuture<OwnershipMemory> asyncRecvSelected(int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before recv, must connect first.");
        }
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<OwnershipMemory>) owner().asyncOperation(sqe -> {
            Instance.LIB_URING.io_uring_prep_recv(sqe, fd, MemorySegment.NULL, len, flag);
            sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
            sqe.setBufGroup(bufferRing.getBufferGroupId());
        }).thenCompose(cqe -> {
            int syscallResult = cqe.getRes();
            if (syscallResult < 0) {
                return CompletableFuture.failedFuture(new SyscallException(syscallResult));
            } else {
                int bid = cqe.getBid();
                IoUringBufferRingElement ringElement = bufferRing.removeBuffer(bid).resultNow();
                return CompletableFuture.completedFuture(IoUringSelectedReadableFd.borrowUringBufferRingElement(ringElement, syscallResult));
            }
        });

    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncSend(OwnershipMemory buffer, int len, int flag) {
        if (!hasConnected) {
            throw new IllegalStateException("before send, must connect first.");
        }
        return ((CancelableFuture<BufferResult<OwnershipMemory>>) owner().asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_send(sqe, fd, buffer.resource(), len, flag))
                .whenComplete(buffer::DropWhenException)
                .thenApply(cqe -> new BufferResult<>(buffer, cqe.getRes())));
    }

    public CancelableFuture<BufferResult<OwnershipMemory>> asyncSendZc(OwnershipMemory buffer, int len, int flag, int zcFlags) {
        if (!hasConnected) {
            throw new IllegalStateException("before send, must connect first.");
        }
        ZCContext context = new ZCContext();
        return (CancelableFuture<BufferResult<OwnershipMemory>>) (new CancelableFuture<BufferResult<OwnershipMemory>>(promise ->
                owner().asyncOperation(
                        sqe -> Instance.LIB_URING.io_uring_prep_send_zc(sqe, fd, buffer.resource(), len, flag, zcFlags),
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

    public boolean bindBufferRing(IoUringBufferRing bufferRing) {
        return BUFFER_RING_VH.compareAndSet(this, null, bufferRing);
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

    static int socketSysCall(SocketAddress address) {
        int domain = switch (address) {
            case UnixDomainSocketAddress _ -> Libc.Socket_H.Domain.AF_UNIX;
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> Libc.Socket_H.Domain.AF_INET;
                case Inet6Address _ -> Libc.Socket_H.Domain.AF_INET6;
                default -> throw new IllegalStateException("Unexpected value: " + inetSocketAddress);
            };
            default -> throw new IllegalStateException("Unexpected value: " + address);
        };
        int type = Libc.Socket_H.Type.SOCK_STREAM;
        int fd = LIBC.socket(domain, type, 0);
        if (fd < 0) {
            throw new IllegalArgumentException("socket error, reason： " + DebugHelper.currentErrorStr());
        }
        try (OwnershipMemory ownershipMemory = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.JAVA_INT.byteSize())) {
            ownershipMemory.resource().set(ValueLayout.JAVA_INT, 0, 1);
            LIBC.setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, ownershipMemory.resource(), (int) ValueLayout.JAVA_INT.byteSize());
        } catch (Exception _) {
        }
        return fd;
    }

    @Override
    public IoUringBufferRing bufferRing() {
        return bufferRing;
    }

    private static class ZCContext {
        Stage stage = Stage.WAIT_MORE;
        int sendRes;

        private enum Stage {
            WAIT_MORE,
            MORE,
            END;
        }
    }

    static {
        try {
            BUFFER_RING_VH = MethodHandles.lookup().findVarHandle(AsyncTcpSocketFd.class, "bufferRing", IoUringBufferRing.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
