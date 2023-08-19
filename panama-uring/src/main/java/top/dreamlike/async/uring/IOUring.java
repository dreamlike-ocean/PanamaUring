package top.dreamlike.async.uring;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.IOOpResult;
import top.dreamlike.common.CType;
import top.dreamlike.common.__kernel_timespec;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.extension.NotEnoughBufferException;
import top.dreamlike.extension.fp.Result;
import top.dreamlike.helper.*;
import top.dreamlike.nativeLib.eventfd.EventFd;
import top.dreamlike.nativeLib.eventfd.eventfd_h;
import top.dreamlike.nativeLib.in.iovec;
import top.dreamlike.nativeLib.in.sockaddr;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.liburing.io_uring;
import top.dreamlike.nativeLib.liburing.io_uring_cqe;
import top.dreamlike.nativeLib.liburing.io_uring_sqe;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.foreign.ValueLayout.*;
import static top.dreamlike.nativeLib.errno.errno_h.ENOBUFS;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_NONBLOCK;
import static top.dreamlike.nativeLib.inet.inet_h.inet_ntoa;
import static top.dreamlike.nativeLib.inet.inet_h.ntohs;
import static top.dreamlike.nativeLib.liburing.liburing_h.*;
import static top.dreamlike.nativeLib.string.string_h.strlen;

/**
 * 不要并发submit和prep
 * 会监听一个eventfd来做wakeUp
 */
@Unsafe("不是线程安全的")
public class IOUring implements AutoCloseable {
    private static final byte[] EMPTY = new byte[0];
    public static final int NO_SQE = -1;
    private MemorySegment ring;

    // 完成事件的eventfd
    private int eventfd = -1;
    Arena allocator;

    private static final int max_loop = 1024;

    private byte gid;

    private Map<Long, IOOpResult> context;

    private final Map<Long, Runnable> cancelCallBack = new HashMap<>();

    private MemorySegment[] selectedBuffer;

    private AtomicLong count;

    private final int ringSize;

    private static final int MIN_RING_SIZE = 16;

    // 用于wakeup的fd
    private EventFd wakeUpFd;
    private MemorySegment wakeUpReadBuffer;

    // todo jdk20之后更换为ScopeLocal实现
    public static final ThreadLocal<Boolean> startLinked = ThreadLocal.withInitial(() -> false);

    private final Queue<Runnable> submitCallBack = new ArrayDeque<>();

    public IOUring(int ringSize) {
        this(ringSize, 16);
    }

    public IOUring(int ringSize, int autoBufferSize) {
        if (ringSize <= 0) {
            throw new IllegalArgumentException("ring size <= 0");
        }
        this.ringSize = Math.max(ringSize, MIN_RING_SIZE);
        initContext();
        initRing();
        initSelectedBuffer(autoBufferSize);
        initWakeEventFd();
    }

    private void initContext() {
        context = new HashMap<>();
        count = new AtomicLong();
    }

    private void initRing() {
        allocator = Arena.openShared();
        this.ring = io_uring.allocate(allocator);
        var ret = io_uring_queue_init(ringSize, ring, 0);
        if (ret < 0) {
            throw new IllegalStateException("io_uring init fail!");
        }
    }

    private void initSelectedBuffer(int autoBufferSize) {
        gid = (byte) new Random(System.currentTimeMillis()).nextInt(100);
        selectedBuffer = new MemorySegment[autoBufferSize];
        for (int i = 0; i < selectedBuffer.length; i++) {
            selectedBuffer[i] = allocator.allocate(1024);
        }

        // fixme CQE溢出问题 当前先不修，因为selectedBuffer api有点复杂
        // 当前先假装不出问题
        for (int i = 0; i < selectedBuffer.length;) {
            if (!provideBuffer(selectedBuffer[i], i, false)) {
                submit();
                continue;
            }
            i++;
        }
        submit();
    }

    private void initWakeEventFd() {
        wakeUpFd = new EventFd();
        wakeUpReadBuffer = allocator.allocate(JAVA_LONG);
        multiShotReadEventfd();
    }

    private void multiShotReadEventfd() {
        BooleanSupplier prepFn = () -> prep_read(wakeUpFd.getFd(), 0, wakeUpReadBuffer, (__) -> {
            System.out.println("Eventfd readable");
            // 轮询到了直接再注册一个可读事件
            multiShotReadEventfd();
        });
        // 不提交 自动提交 -> select(-1)
        while (!prepFn.getAsBoolean()) {
            submit();
        }
    }

    @Deprecated
    public void registerToEpoll(Epoll epoll) {
        // int uring_event_fd = eventfd_h.eventfd(0, EFD_NONBLOCK());
        // if (eventfd != -1) {
        // io_uring_unregister_eventfd(ring);
        // }
        // eventfd = uring_event_fd;
        // io_uring_register_eventfd(ring, eventfd);
        // epoll.register(eventfd, EPOLLIN());
    }

    public synchronized int registerEventFd() {
        if (eventfd != -1) {
            return eventfd;
        }
        int uring_event_fd = eventfd_h.eventfd(0, EFD_NONBLOCK());
        if (eventfd != -1) {
            io_uring_unregister_eventfd(ring);
        }
        eventfd = uring_event_fd;
        io_uring_register_eventfd(ring, uring_event_fd);
        return uring_event_fd;
    }

    public boolean checkSupport(int op) {
        return io_uring_opcode_supported_ring(ring, op) == 1;
    }

    public long prep_selected_read_and_get_user_data(int fd, int offset, int length,
                                                     Consumer<Result<byte[], Throwable>> consumer) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        try (Arena allocator = Arena.openConfined()) {
            MemorySegment iovecStruct = iovec.allocate(allocator);
            iovec.iov_len$set(iovecStruct, length);
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
            IOOpResult result = new IOOpResult(fd, -1, Op.FILE_SELECTED_READ, null, (res, bid) -> {
                if (res == -ENOBUFS()) {
                    var t = new NotEnoughBufferException(gid);
                    consumer.accept(new Result.Err<>(t));
                    return;
                }

                if (res < 0) {
                    var t = new NativeCallException(NativeHelper.getErrorStr(-res));
                    consumer.accept(new Result.Err<>(t));
                    return;
                }

                MemorySegment buffer = selectedBuffer[bid];
                byte[] readRes = buffer.asSlice(0, res).toArray(JAVA_BYTE);
                consumer.accept(new Result.OK<>(readRes));
                provideBuffer(buffer, bid, true);
            });
            long opsCount = count.getAndIncrement();

            context.put(opsCount, result);
            io_uring_prep_readv(sqe, fd, iovecStruct, 1, offset);
            io_uring_sqe.flags$and(sqeSegment, (byte) IOSQE_BUFFER_SELECT());
            io_uring_sqe.buf_group$set(sqeSegment, gid);
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            return opsCount;
        }
    }

    public boolean prep_selected_read(int fd, int offset, int length, CompletableFuture<byte[]> readBufferPromise) {
        return prep_selected_read_and_get_user_data(fd, offset, length, Result.transform(readBufferPromise)) != NO_SQE;
    }

    public boolean prep_selected_recv(int socketFd, int length, CompletableFuture<byte[]> recvBufferPromise) {
        return prep_selected_recv_and_get_user_data(socketFd, length, recvBufferPromise) != NO_SQE;
    }

    public long prep_selected_recv_and_get_user_data(int socketFd, int length,
            CompletableFuture<byte[]> recvBufferPromise) {
        return prep_selected_recv_and_get_user_data(socketFd, length, Result.transform(recvBufferPromise));
    }

    public long prep_selected_recv_and_get_user_data(int socketFd, int length,
                                                     Consumer<Result<byte[], Throwable>> consumer) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        MemorySegment sqeStruct = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();
        IOOpResult result = new IOOpResult(socketFd, -1, Op.SOCKET_SELECTED_READ, null, (res, bid) -> {
            if (res == -ENOBUFS()) {
                consumer.accept(new Result.Err<>(new Exception(String.format("gid: %d 不存在空余的内存了", gid))));
                return;
            }
            MemorySegment buffer = selectedBuffer[bid];
            consumer.accept(new Result.OK<>(buffer.asSlice(0, res).toArray(ValueLayout.JAVA_BYTE)));
            provideBuffer(buffer, bid, true);
        });
        io_uring_prep_recv(sqe, socketFd, MemorySegment.NULL, length, 0);
        io_uring_sqe.flags$and(sqeStruct, (byte) IOSQE_BUFFER_SELECT());
        io_uring_sqe.user_data$set(sqeStruct, opsCount);
        io_uring_sqe.buf_group$set(sqeStruct, gid);
        context.put(opsCount, result);
        return opsCount;
    }

    public boolean prep_recv(int socketFd, MemorySegment buf, IntConsumer consumer) {
        return prep_recv_and_get_user_data(socketFd, buf, consumer) != NO_SQE;
    }

    public long prep_recv_and_get_user_data(int socketFd, MemorySegment buf, IntConsumer consumer) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        MemorySegment sqeStruct = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();
        io_uring_prep_recv(sqe, socketFd, buf, buf.byteSize(), 0);
        io_uring_sqe.user_data$set(sqeStruct, opsCount);
        context.put(opsCount, new IOOpResult(socketFd, -1, Op.SOCKET_READ, buf, (res, __) -> consumer.accept(res)));
        return opsCount;
    }

    public boolean prep_recv_multi(int socketFd, BiConsumer<byte[], Throwable> consumer) {
        return prep_recv_multi_and_get_user_data(socketFd, consumer) != NO_SQE;
    }

    public long prep_recv_multi_and_get_user_data(int socketFd, BiConsumer<byte[], Throwable> consumer) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        long opsCount = count.getAndIncrement();
        context.put(opsCount, new IOOpResult(socketFd, -1, Op.MULTI_SHOT_RECV, null, (res, bid) -> {
            if (res == -ENOBUFS()) {
                consumer.accept(EMPTY, new NotEnoughBufferException(gid));
                return;
            }
            if (res < 0) {
                consumer.accept(EMPTY, new NativeCallException(NativeHelper.getErrorStr(-res)));
            }
            MemorySegment buffer = selectedBuffer[bid];
            byte[] heapBuffer = res == 0 ? EMPTY : buffer.asSlice(0, res).toArray(JAVA_BYTE);
            consumer.accept(heapBuffer, null);
            provideBuffer(buffer, bid, true);
        }));
        io_uring_prep_recv_multi(sqe, socketFd, MemorySegment.NULL, 0, 0);
        io_uring_sqe.user_data$set(sqe, opsCount);
        io_uring_sqe.flags$and(sqe, (byte) IOSQE_BUFFER_SELECT());
        io_uring_sqe.buf_group$set(sqe, gid);

        return opsCount;
    }

    public boolean prep_send(int socketFd, MemorySegment buffer, IntConsumer callback) {
        return prep_send_and_get_user_data(socketFd, buffer, callback) != NO_SQE;
    }

    public long prep_send_and_get_user_data(int socketFd, MemorySegment buffer, IntConsumer callback) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();

        context.put(opsCount, new IOOpResult(socketFd, -1, Op.SOCKET_WRITE, buffer, (res, __) -> callback.accept(res)));
        io_uring_prep_send(sqe, socketFd, buffer, buffer.byteSize(), 0);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);

        return opsCount;
    }

    public boolean prep_accept(int serverFd, Consumer<SocketInfo> callback) {
        return prep_accept_and_get_user_data(serverFd, callback) != NO_SQE;
    }

    static {

        if (!NativeHelper.isLinux() || !NativeHelper.isX86_64()
                || !NativeHelper.compareWithCurrentLinuxVersion(5, 10)) {
            throw new NativeCallException("please check os version(need linux >= 5.10) and CPU Arch(need X86_64)");
        }
        NativeHelper.loadSo("liburing-ffi.2.4");

        AccessHelper.fetchContext = uring -> uring.context;
    }

    public long prep_accept_and_get_user_data(int serverFd, Consumer<SocketInfo> callback) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        Arena tmp = Arena.openShared();
        MemorySegment client_addr = sockaddr.allocate(tmp);
        MemorySegment client_addr_len = tmp.allocate(JAVA_INT, (int) sockaddr.sizeof());

        long opsCount = count.getAndIncrement();
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());

        context.put(opsCount, new IOOpResult(serverFd, -1, Op.ACCEPT, null, (res, __) -> {
            try {
                if (res < 0) {
                    callback.accept(new SocketInfo(res, "", -1));
                } else {
                    short sin_port = sockaddr_in.sin_port$get(client_addr);
                    int port = Short.toUnsignedInt(ntohs(sin_port));
                    MemorySegment remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                    long strlen = strlen(remoteHost);
                    String host = new String(MemorySegment.ofAddress(remoteHost.address(), strlen).toArray(JAVA_BYTE));
                    callback.accept(new SocketInfo(res, host, port));
                }
            } finally {
                tmp.close();
            }

        }));
        io_uring_prep_accept(sqe, serverFd, client_addr, client_addr_len, 0);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);

        return opsCount;
    }

    public boolean prep_cancel(long user_data, int cancel_flags, IntConsumer consumer) {
        return prep_cancel_and_get_user_data(user_data, cancel_flags, consumer) != NO_SQE;
    }

    public boolean prep_accept_multi(int serverFd, Consumer<SocketInfo> callback) {
        return prep_accept_multi_and_get_user_data(serverFd, callback) != NO_SQE;
    }

    public long prep_cancel_and_get_user_data(long need_cancel_user_data, int cancel_flags, IntConsumer consumer) {
        MemorySegment sqe = null;
        while (sqe == null) {
            sqe = getSqe();
            if (sqe == null) {
                submit();
            }
        }
        long opsCount = count.getAndIncrement();
        MemorySegment sqeStruct = NativeHelper.unsafePointConvertor(sqe);
        context.put(opsCount, IOOpResult.bindCallBack(Op.CANCEL, (res, __) -> {
            if (res >= 0) {
                Runnable canncelCallback = cancelCallBack.remove(need_cancel_user_data);
                if (canncelCallback != null) {
                    canncelCallback.run();
                }
                context.remove(need_cancel_user_data);
            }
            consumer.accept(res);
        }));
        io_uring_prep_cancel(sqe, need_cancel_user_data, cancel_flags);
        io_uring_sqe.user_data$set(sqeStruct, opsCount);
        return opsCount;
    }

    public boolean prep_read(int fd, int offset, MemorySegment buffer, IntConsumer callback) {
        return prep_read_and_get_user_data(fd, offset, buffer, callback) != NO_SQE;
    }

    public long prep_read_and_get_user_data(int fd, int offset, MemorySegment buffer, IntConsumer callback) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        // byte*,len
        try (Arena allocator = Arena.openConfined()) {
            MemorySegment iovecStruct = iovec.allocate(allocator);
            iovec.iov_base$set(iovecStruct, buffer);
            iovec.iov_len$set(iovecStruct, buffer.byteSize());
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
            long opsCount = count.getAndIncrement();
            io_uring_prep_read(sqe, fd, buffer, (int) buffer.byteSize(), offset);
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(fd, -1, Op.FILE_READ, buffer, (res, __) -> callback.accept(res)));
            return opsCount;
        }
    }

    public boolean prep_time_out(long waitMs, Runnable runnable) {
        return prep_time_out_and_get_user_data(waitMs, runnable) != NO_SQE;
    }

    public long prep_time_out_and_get_user_data(long waitMs, Runnable runnable) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        try (Arena allocator = Arena.openConfined()) {
            MemorySegment ts = __kernel_timespec.allocate(allocator);
            __kernel_timespec.tv_sec$set(ts, waitMs / 1000);
            __kernel_timespec.tv_nsec$set(ts, (waitMs % 1000) * 1000000);
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());

            long opsCount = count.getAndIncrement();
            io_uring_prep_timeout(sqeSegment, ts, 0, 0);
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(-1, -1, Op.TIMEOUT, null, (res, __) -> runnable.run()));
            return opsCount;
        }

    }

    protected MemorySegment getSqe() {
        return getSqe(false);
    }

    protected MemorySegment getSqe(boolean loopUntilGet) {
        int i = 0;
        MemorySegment sqe = MemorySegment.NULL;
        // 空指针
        while (sqe.address() == 0) {
            sqe = io_uring_get_sqe(ring);
            i++;
            //持续max次还没有获取到sqe 且不允许loopUntilGet 直接返回null
            if (i >= max_loop && sqe.address() == 0 && !loopUntilGet) {
                return null;
            }
            if (sqe.address() == 0) {
                submit();
            }
        }

        if (isStartLinked()) {
            io_uring_sqe.flags$and(NativeHelper.unsafePointConvertor(sqe), (byte) IOSQE_IO_LINK());
        }
        sqe = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        return sqe;
    }
    // sqe -> cqe

    public long prep_write_and_get_user_data(int fd, int offset, MemorySegment buffer, IntConsumer callback) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();
        io_uring_prep_write(sqe, fd, buffer, (int) buffer.byteSize(), offset);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(fd, -1, Op.FILE_WRITE, buffer, (res, __) -> callback.accept(res)));
        return opsCount;
    }

    public boolean prep_write(int fd, int offset, MemorySegment buffer, IntConsumer callback) {
        return prep_write_and_get_user_data(fd, offset, buffer, callback) != NO_SQE;
    }

    public boolean prep_no_op(Runnable runnable) {
        return prep_no_op_and_get_user_data(runnable) != NO_SQE;
    }

    public long prep_no_op_and_get_user_data(Runnable runnable) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();
        io_uring_prep_nop(sqeSegment);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(-1, -1, Op.NO, null, (res, __) -> runnable.run()));
        return opsCount;
    }

    public long prep_fsync_and_get_user_data(int fd, int fsyncflag, Consumer<Integer> runnable) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        long opsCount = count.getAndIncrement();

        context.put(opsCount, new IOOpResult(-1, -1, Op.FILE_SYNC, null, (res, __) -> runnable.accept(res)));
        io_uring_prep_fsync(sqe, fd, fsyncflag);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        return opsCount;
    }

    public boolean prep_fsync(int fd, int fsyncflag, Consumer<Integer> runnable) {
        return prep_fsync_and_get_user_data(fd, fsyncflag, runnable) != NO_SQE;
    }

    public long prep_connect_and_get_user_data(SocketInfo info, IntConsumer callback) throws UnknownHostException {
        int fd = info.res();
        MemorySegment sqe;
        if (fd == -1 || (sqe = getSqe()) == null) {
            return NO_SQE;
        }
        InetSocketAddress inetAddress = InetSocketAddress.createUnresolved(info.host(), info.port());
        Arena session = Arena.openConfined();
        String host = inetAddress.getHostString();
        int port = inetAddress.getPort();
        Pair<MemorySegment, Boolean> socketInfo = NativeHelper.getSockAddr(session, host, port);
        MemorySegment sockaddrSegement = socketInfo.t1();
        long opsCount = count.getAndIncrement();
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        context.put(opsCount, new IOOpResult(-1, -1, Op.CONNECT, null, (res, __) -> callback.accept(res)));
        io_uring_prep_connect(sqe, fd, sockaddrSegement, (int) sockaddrSegement.byteSize());
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        submitCallBack.offer(session::close);
        return opsCount;
    }

    public boolean prep_connect(SocketInfo info, IntConsumer callback) throws UnknownHostException {
        return prep_connect_and_get_user_data(info, callback) != NO_SQE;
    }

    public void wakeup() {
        wakeUpFd.write(1);
    }

    public boolean provideBuffer(MemorySegment buffer, int bid, boolean needSubmit) {
        MemorySegment sqe = getSqe();
        if (sqe == null) {
            return false;
        }

        io_uring_prep_provide_buffers(sqe, buffer, (int) buffer.byteSize(), 1, gid, bid);

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());
        io_uring_sqe.user_data$set(sqeSegment, -IORING_OP_PROVIDE_BUFFERS());
        if (needSubmit) {
            submit();
        }

        return true;
    }

    public int submit() {
        int i = io_uring_submit(ring);
        while (!submitCallBack.isEmpty()) {
            submitCallBack.poll().run();
        }
        return i;
    }

    /**
     * 非阻塞获取事件
     *
     * @param max 本次最大获取事件数量
     * @return
     */
    public List<IOOpResult> batchGetCqe(int max) {
        try (Arena tmp = Arena.openConfined()) {
            MemorySegment ref = tmp.allocate(CType.C_POINTER$LAYOUT.byteSize());
            int count = 0;
            ArrayList<IOOpResult> results = new ArrayList<>();
            while (count <= max && io_uring_peek_cqe(ring, ref) == 0) {
                MemorySegment cqePoint = ref.get(CType.C_POINTER$LAYOUT, 0);
                MemorySegment cqe = NativeHelper.unsafePointConvertor(cqePoint);
                long opsCount = io_uring_cqe.user_data$get(cqe);
                if (opsCount < 0) {
                    io_uring_cqe_seen(ring, cqe);
                    continue;
                }
                int flag = io_uring_cqe.flags$get(cqe);
                boolean isMultiOp = (flag & IORING_CQE_F_MORE()) != 0;
                IOOpResult result = isMultiOp ? context.get(opsCount) : context.remove(opsCount);
                if (result == null) {
                    System.err.println("result is null,ops:" + opsCount);
                    io_uring_cqe_seen(ring, cqe);
                    continue;
                }
                result.flag = flag;
                result.userData = opsCount;
                result.res = io_uring_cqe.res$get(cqe);
                result.bid = flag >> IORING_CQE_BUFFER_SHIFT();
                io_uring_cqe_seen(ring, cqe);
                results.add(result);
            }
            return results;
        }

    }

    /**
     * 等待一个事件
     *
     * @return
     */
    public List<IOOpResult> waitFd() {
        // 及时释放内存
        // 太弱智了 不支持栈上分配
        try (Arena tmp = Arena.openConfined()) {
            MemorySegment ref = tmp.allocate(CType.C_POINTER$LAYOUT.byteSize());
            io_uring_wait_cqe(ring, ref);
            MemorySegment cqePoint = ref.get(CType.C_POINTER$LAYOUT, 0);
            MemorySegment cqe = NativeHelper.unsafePointConvertor(cqePoint);
            long opsCount = io_uring_cqe.user_data$get(cqe);
            if (opsCount < 0) {
                // 非fd
                io_uring_cqe_seen(ring, cqe);
                return List.of();
            }
            int flag = io_uring_cqe.flags$get(cqe);
            boolean isMultiOp = (flag & IORING_CQE_F_MORE()) != 0;
            IOOpResult result = isMultiOp ? context.get(opsCount) : context.remove(opsCount);

            result.res = io_uring_cqe.res$get(cqe);
            result.bid = io_uring_cqe.flags$get(cqe) >> IORING_CQE_BUFFER_SHIFT();
            io_uring_cqe_seen(ring, cqe);
            return List.of(result);
        }

    }

    public void waitComplete() {
        waitComplete(-1);
    }

    public long prep_accept_multi_and_get_user_data(int serverFd, Consumer<SocketInfo> callback) {
        MemorySegment sqe = getSqe();
        if (sqe == null)
            return NO_SQE;
        Arena tmp = Arena.openConfined();
        MemorySegment client_addr = sockaddr.allocate(tmp);
        MemorySegment client_addr_len = tmp.allocate(JAVA_INT, (int) sockaddr.sizeof());

        long opsCount = count.getAndIncrement();
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe.address(), io_uring_sqe.sizeof());

        context.put(opsCount, new IOOpResult(serverFd, -1, Op.MULTI_SHOT_ACCEPT, null, (res, __) -> {
            if (res < 0) {
                callback.accept(new SocketInfo(res, "", -1));
            } else {
                short sin_port = sockaddr_in.sin_port$get(client_addr);
                int port = Short.toUnsignedInt(ntohs(sin_port));
                MemorySegment remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                long strlen = strlen(remoteHost);
                String host = new String(MemorySegment.ofAddress(remoteHost.address(), strlen).toArray(JAVA_BYTE));
                callback.accept(new SocketInfo(res, host, port));
            }
        }));
        io_uring_prep_multishot_accept(sqe, serverFd, client_addr, client_addr_len, 0);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        cancelCallBack.put(opsCount, tmp::close);

        return opsCount;
    }

    public void waitComplete(long waitMs) {
        try (Arena session = Arena.openConfined()) {
            MemorySegment cqe_ptr = session.allocate(CType.C_POINTER$LAYOUT.byteSize());
            if (waitMs == -1) {
                io_uring_wait_cqe(ring, cqe_ptr);
                return;
            }

            MemorySegment ts = __kernel_timespec.allocate(allocator);
            __kernel_timespec.tv_sec$set(ts, waitMs / 1000);
            __kernel_timespec.tv_nsec$set(ts, (waitMs % 1000) * 1000000);
            io_uring_wait_cqe_timeout(ring, cqe_ptr, ts);
        }
    }

    @Override
    public void close() throws Exception {
        io_uring_queue_exit(ring);
        allocator.close();
    }

    private boolean isStartLinked() {
        return startLinked.get();
    }

    public boolean contain(long userData) {
        return context.containsKey(userData);
    }

}
