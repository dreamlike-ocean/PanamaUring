package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrIn6;
import top.dreamlike.panama.uring.nativelib.struct.socket.SocketAddrUn;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AsyncTcpServerFd implements IoUringAsyncFd {

    private final static Libc LIBC = Instance.LIBC;

    private IoUringEventLoop owner;

    private final int fd;

    private final SocketAddress address;

    private final int port;

    private volatile boolean hasListen;

    private volatile Supplier<IoUringEventLoop> subSocketEventLoopBinder = () -> owner;

    public AsyncTcpServerFd(IoUringEventLoop owner, SocketAddress address, int port) {
        this.owner = owner;
        this.address = address;
        this.port = port;
        int domain = switch (address) {
            case UnixDomainSocketAddress _ -> Libc.Socket_H.Domain.AF_UNIX;
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> Libc.Socket_H.Domain.AF_INET;
                case Inet6Address _ -> Libc.Socket_H.Domain.AF_INET6;
                default -> throw new IllegalStateException(STR."Unexpected value: \{inetSocketAddress}");
            };
            default -> throw new IllegalStateException(STR."Unexpected value: \{address}");
        };
        int type = Libc.Socket_H.Type.SOCK_STREAM;
        this.fd = LIBC.socket(domain, type, 0);
        if (fd < 0) {
            throw new IllegalArgumentException(STR."socket error, reason\{DebugHelper.currentErrorStr()}");
        }
        this.hasListen = false;
    }

    public int bind() {
        OwnershipMemory addr = switch (address) {
            case UnixDomainSocketAddress address -> udsAddress(address);
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address v4 -> ipv4Address(v4, port);
                case Inet6Address v6 -> ipv6Address(v6, port);
                default -> throw new IllegalStateException(STR."Unexpected value: \{inetSocketAddress.getAddress()}");
            };
            default -> throw new IllegalStateException(STR."Unexpected value: \{address}");
        };
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
        return switch (address) {
            case UnixDomainSocketAddress _ -> SocketAddrUn.LAYOUT.byteSize();
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> SocketAddrIn.LAYOUT.byteSize();
                case Inet6Address _ -> SocketAddrIn6.LAYOUT.byteSize();
                default -> throw new IllegalStateException(STR."Unexpected value: \{inetSocketAddress.getAddress()}");
            };
            default -> throw new IllegalStateException(STR."Unexpected value: \{address}");
        };
    }

    public CancelableFuture<AsyncTcpSocket> asyncAccept(int flag, OwnershipMemory sockaddr, OwnershipMemory sockLen) {
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
        sockLenMemory.set(ValueLayout.JAVA_INT,0L, (int) sockaddrMemory.byteSize());

        return (CancelableFuture<AsyncTcpSocket>) owner.asyncOperation(sqe -> Instance.LIB_URING.io_uring_prep_accept(sqe, fd, sockaddrMemory, sockLenMemory, flag))
                .thenApply(cqe -> {
                    try(sockaddr; sockLen) {
                        int acceptFd = cqe.getRes();
                        if (acceptFd < 0) {
                            throw new IllegalArgumentException(STR."accept fail, reason: \{DebugHelper.getErrorStr(-acceptFd)}");
                        }
                        SocketAddress remoteAddress = switch (address) {
                            case UnixDomainSocketAddress _ -> address;
                            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                                case Inet4Address _ -> parseV4(sockaddrMemory);
                                case Inet6Address _ -> parseV6(sockaddrMemory);
                                default ->
                                        throw new IllegalStateException(STR."Unexpected value: \{inetSocketAddress.getAddress()}");
                            };
                            default -> throw new IllegalStateException(STR."Unexpected value: \{address}");
                        };
                        IoUringEventLoop subEventLoop = subSocketEventLoopBinder.get();
                        return new AsyncTcpSocket(subEventLoop, acceptFd, address, remoteAddress);
                    }catch (Exception exception) {
                        //不应该在这里抛出异常 无视就行了
                        throw new IllegalArgumentException(exception);
                    }
                });
    }


    public void setSubSocketEventLoopBinder(Supplier<IoUringEventLoop> subSocketEventLoopBinder) {
        Objects.requireNonNull(subSocketEventLoopBinder, "subSocketEventLoopBinder can not be null");
        this.subSocketEventLoopBinder = subSocketEventLoopBinder;
    }

    private OwnershipMemory ipv6Address(Inet6Address inet6Address, int port) {
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

    private SocketAddress parseV6(MemorySegment sockaddrMemory) {
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

    private SocketAddress parseV4(MemorySegment sockaddrMemory) {
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

    public OwnershipMemory ipv4Address(Inet4Address inet4Address, int port) {
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

    public OwnershipMemory udsAddress(UnixDomainSocketAddress unixDomainSocketAddress) {
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

    @Override
    public IoUringEventLoop owner() {
        return owner;
    }

    @Override
    public int fd() {
        return fd;
    }


    @Override
    public CancelableFuture<Integer> asyncWrite(OwnershipMemory buffer, int len, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<Integer> asyncWriteV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<Integer> asyncReadV(OwnershipResource<NativeArrayPointer<Iovec>> iovec, int nr_vecs, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<IoUringCqe> asyncSelectedRead(int len, int offset, short bufferGroupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CancelableFuture<Integer> asyncRead(OwnershipMemory buffer, int len, int offset) {
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
