package top.dreamlike.async.uring;

import top.dreamlike.async.IOOpResult;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.helper.*;
import top.dreamlike.nativeLib.eventfd.EventFd;
import top.dreamlike.nativeLib.eventfd.eventfd_h;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.liburing.*;
import top.dreamlike.nativeLib.socket.sockaddr;

import java.io.*;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.foreign.ValueLayout.*;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_NONBLOCK;
import static top.dreamlike.nativeLib.inet.inet_h.C_POINTER;
import static top.dreamlike.nativeLib.inet.inet_h.*;
import static top.dreamlike.nativeLib.liburing.liburing_h.*;
import static top.dreamlike.nativeLib.string.string_h.strlen;

/**
 * 不要并发submit和prep
 * 会监听一个eventfd来做wakeUp
 */
@Unsafe("不是线程安全的")
public class IOUring implements AutoCloseable {
    private MemorySegment ring;

    //完成事件的eventfd
    private int eventfd = -1;
    MemorySession allocator;

    private static final int max_loop = 1024;

    private byte gid;

    private Map<Long, IOOpResult> context;

    private MemorySegment[] selectedBuffer;

    private AtomicLong count;

    private final int ringSize;

    private static final int MIN_RING_SIZE = 16;

    //用于wakeup的fd
    private EventFd wakeUpFd;
    private MemorySegment wakeUpReadBuffer;


    public IOUring(int ringSize){
        this(ringSize,16);
    }
    public IOUring(int ringSize,int autoBufferSize){
        if (ringSize <= 0) {
            throw new IllegalArgumentException("ring size <= 0");
        }
        if (ringSize < MIN_RING_SIZE) {
            this.ringSize = MIN_RING_SIZE;
        }else  {
            this.ringSize = ringSize;
        }
        initContext();
        initRing();
        initSelectedBuffer(autoBufferSize);
        initWakeEventFd();
    }

    private void initContext(){
        context = new ConcurrentHashMap<>();
        count = new AtomicLong();
    }
    private void initRing(){
        allocator = MemorySession.openShared();
        this.ring = io_uring.allocate(allocator);
        var ret= io_uring_queue_init(ringSize, ring, 0);
        if (ret < 0){
            throw new IllegalStateException("io_uring init fail!");
        }
    }

    private void initSelectedBuffer(int autoBufferSize){
        gid =(byte) new Random(System.currentTimeMillis()).nextInt(100);
        selectedBuffer = new MemorySegment[autoBufferSize];
        for (int i = 0; i < selectedBuffer.length; i++) {
            selectedBuffer[i] = allocator.allocate(1024);
        }

        //fixme CQE溢出问题 当前先不修，因为selectedBuffer api有点复杂
        //      当前先假装不出问题
        for (int i = 0; i < selectedBuffer.length;) {
            if (!provideBuffer(selectedBuffer[i],i,false)) {
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

    private void multiShotReadEventfd(){
        prep_read(wakeUpFd.getFd(), 0, wakeUpReadBuffer,(__) -> {
            //轮询到了直接再注册一个可读事件
            wakeUpFd.read(wakeUpReadBuffer);
            multiShotReadEventfd();
        });
        submit();
    }

    public void registerToEpoll(Epoll epoll){
        int uring_event_fd = eventfd_h.eventfd(0, EFD_NONBLOCK());
        if (eventfd !=-1) {
            io_uring_unregister_eventfd(ring);
        }
        eventfd = uring_event_fd;
        io_uring_register_eventfd(ring,eventfd);
        epoll.register(eventfd, EPOLLIN());
        System.out.println("register :"+eventfd);
    }



    public boolean checkSupport(int op){
        return io_uring_opcode_supported_ring(ring, op) == 1;
    }


    public boolean prep_selected_read(int fd, int offset, int length, CompletableFuture<byte[]> readBufferPromise){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment iovecStruct = iovec.allocate(allocator);
            iovec.iov_len$set(iovecStruct,length);
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());

            io_uring_sqe.flags$set(sqeSegment, 0L, ((byte) (io_uring_sqe.flags$get(sqeSegment) | (byte) IOSQE_BUFFER_SELECT())));

            io_uring_sqe.buf_group$set(sqeSegment, gid);
            IOOpResult result = new IOOpResult(fd, -1,Op.FILE_SELECTED_READ, null, (res,bid) -> {
                if (res == -ENOBUFS()) {
                    readBufferPromise.completeExceptionally(new NativeCallException(String.format("gid: %d 不存在空余的内存了", gid)));
                    return;
                }

                if (res < 0) {
                    readBufferPromise.completeExceptionally(new NativeCallException(NativeHelper.getErrorStr(-res)));
                    return;
                }

                MemorySegment buffer = selectedBuffer[bid];
                readBufferPromise.complete(buffer.asSlice(0, res).toArray(ValueLayout.JAVA_BYTE));
                provideBuffer(buffer, bid, true);
            });
            long opsCount = count.getAndIncrement();

            context.put(opsCount, result);

            io_uring_prep_readv(sqe,fd,iovecStruct, 1, offset);
            io_uring_sqe.user_data$set(sqeSegment,opsCount);
        }

        return true;
    }

    public boolean prep_selected_recv(int socketFd, int length, CompletableFuture<byte[]> recvBufferPromise) {
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());

        io_uring_sqe.flags$set(sqeSegment, 0L, ((byte) (io_uring_sqe.flags$get(sqeSegment) | (byte) IOSQE_BUFFER_SELECT())));

        io_uring_sqe.buf_group$set(sqeSegment, gid);
        long opsCount = count.getAndIncrement();
        IOOpResult result = new IOOpResult(socketFd, -1, Op.SOCKET_SELECTED_READ, null, (res, bid) -> {
            if (res == -ENOBUFS()) {
                recvBufferPromise.completeExceptionally(new Exception(String.format("gid: %d 不存在空余的内存了", gid)));
                return;
            }
            MemorySegment buffer = selectedBuffer[bid];
            recvBufferPromise.complete(buffer.asSlice(0, res).toArray(ValueLayout.JAVA_BYTE));
            provideBuffer(buffer, bid,true);
        });
        io_uring_prep_recv(sqe,socketFd, MemoryAddress.NULL, length,0);
        io_uring_sqe.user_data$set(sqeSegment,opsCount);
        context.put(opsCount, result);


        return true;
    }

    public boolean prep_send(int socketFd, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        long opsCount = count.getAndIncrement();

        context.put(opsCount, new IOOpResult(socketFd, -1,Op.SOCKET_WRITE, buffer, (res, __) -> callback.accept(res)));
        io_uring_prep_send(sqe,socketFd,buffer,buffer.byteSize(), 0);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);

        return true;
    }


    public boolean prep_accept(int serverFd, Consumer<SocketInfo> callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySession tmp = MemorySession.openShared();
        MemorySegment client_addr = sockaddr.allocate(tmp);
        MemorySegment client_addr_len = tmp.allocate(JAVA_INT,(int) sockaddr.sizeof());

        long opsCount = count.getAndIncrement();
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());

        context.put(opsCount, new IOOpResult(serverFd, -1,Op.ACCEPT, null, (res, __) -> {
            if (res < 0){
                callback.accept(new SocketInfo(-1,"", -1));
            }else {
                short sin_port = sockaddr_in.sin_port$get(client_addr);
                int port = Short.toUnsignedInt(ntohs(sin_port));
                MemoryAddress remoteHost = inet_ntoa(sockaddr_in.sin_addr$slice(client_addr));
                long strlen = strlen(remoteHost);
                String host = new String(MemorySegment.ofAddress(remoteHost, strlen, MemorySession.global()).toArray(JAVA_BYTE));
                callback.accept(new SocketInfo(res,host,port));
            }
            tmp.close();
        }));
        io_uring_prep_accept(sqe,serverFd,client_addr,client_addr_len,0);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);

        return true;
    }

    public boolean prep_read(int fd, int offset, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        //byte*,len
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment iovecStruct = iovec.allocate(allocator);
            iovec.iov_base$set(iovecStruct, buffer.address());
            iovec.iov_len$set(iovecStruct, buffer.byteSize());
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
            long opsCount = count.getAndIncrement();
            io_uring_prep_read(sqe,fd,buffer,(int) buffer.byteSize(), offset);
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(fd, -1,Op.FILE_READ, buffer, (res, __) -> callback.accept(res)));
            return true;
        }
    }

    public boolean prep_time_out(long waitMs,Runnable runnable){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        try (MemorySession allocator = MemorySession.openConfined()) {
            MemorySegment ts = __kernel_timespec.allocate(allocator);
            __kernel_timespec.tv_sec$set(ts,waitMs/1000);
            __kernel_timespec.tv_nsec$set(ts, (waitMs % 1000) * 1000000);
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());

            long opsCount = count.getAndIncrement();
            io_uring_prep_timeout(sqeSegment,ts,0,0);
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(-1, -1,Op.TIMEOUT, null, (res, __) -> runnable.run()));
        }
        return true;
    }

    protected MemoryAddress getSqe() {
        int i = 0;
        MemoryAddress sqe = MemoryAddress.NULL;
        //空指针
        while (sqe.toRawLongValue() == 0){
            sqe = io_uring_get_sqe(ring);
            i++;
            if (i >= max_loop && sqe.toRawLongValue() == 0) {
                return null;
            }
        }
        return sqe;
    }
//sqe -> cqe

    public boolean prep_write(int fd, int offset, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        long opsCount = count.getAndIncrement();
        io_uring_prep_write(sqe, fd, buffer, (int) buffer.byteSize(), offset);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(fd, -1,Op.FILE_WRITE, buffer, (res, __) -> callback.accept(res)));
        return true;
    }

    public boolean prep_no_op(Runnable runnable){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        long opsCount = count.getAndIncrement();
        io_uring_prep_nop(sqeSegment);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(-1, -1, Op.NO,null, (res, __) -> runnable.run()));
        return true;
    }

    public boolean prep_fsync(int fd,int fsyncflag,Consumer<Integer> runnable){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        long opsCount = count.getAndIncrement();

        context.put(opsCount, new IOOpResult(-1, -1,Op.FILE_SYNC, null, (res, __) -> runnable.accept(res)));
        io_uring_prep_fsync(sqe, fd,fsyncflag);
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        return true;
    }

    public boolean prep_connect(SocketInfo info, IntConsumer callback) throws UnknownHostException {
        int fd = info.fd();
        MemoryAddress sqe;
        if (fd == -1 || (sqe = getSqe()) == null) {
            return false;
        }
        InetSocketAddress inetAddress = InetSocketAddress.createUnresolved(info.host(), info.port());
        try (MemorySession session = MemorySession.openConfined()) {
            String host = inetAddress.getHostString();
            int port = inetAddress.getPort();
            Pair<MemorySegment, Boolean> socketInfo = NativeHelper.getSockAddr(session, host, port);
            MemorySegment sockaddrSegement = socketInfo.t1();
            long opsCount = count.getAndIncrement();
            MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
            context.put(opsCount, new IOOpResult(-1, -1, Op.CONNECT, null, (res, __) -> callback.accept(res)));
            io_uring_prep_connect(sqe, fd, sockaddrSegement, (int) sockaddrSegement.byteSize());
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            //todo 如果不提交则会导致异步的submit时sockaddr被释放了导致入参不对
            //思考一下能不能不提交
            submit();
        }
        return true;
    }

   

    public void wakeup(){
        wakeUpFd.write(1);
    }

    public boolean provideBuffer(MemorySegment buffer,int bid,boolean needSubmit){
        MemoryAddress sqe = getSqe();
        if (sqe == null) {
            return false;
        }

        io_uring_prep_provide_buffers(sqe, buffer,(int) buffer.byteSize(),1,gid, bid);

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_sqe.user_data$set(sqeSegment, -IORING_OP_PROVIDE_BUFFERS());
        if (needSubmit){
            submit();
        }

        return true;
    }

    public int submit(){
        return io_uring_submit(ring);
    }


    /**
     * 非阻塞获取事件
     * @param max 本次最大获取事件数量
     * @return
     */
    public List<IOOpResult> batchGetCqe(int max){
        try (MemorySession tmp = MemorySession.openConfined()) {
            MemorySegment ref = tmp.allocate(C_POINTER.byteSize());
            int count = 0;
            ArrayList<IOOpResult> results = new ArrayList<>();
            while (count <= max && io_uring_peek_cqe(ring, ref) == 0) {
                MemoryAddress cqePoint = ref.get(C_POINTER, 0);
                MemorySegment cqe = io_uring_cqe.ofAddress(cqePoint, MemorySession.global());
                long opsCount = io_uring_cqe.user_data$get(cqe);
                if (opsCount < 0){
                    io_uring_cqe_seen(ring,cqe);
                    continue;
                }
                IOOpResult result = context.remove(opsCount);
                if (result == null) {
                    System.err.println("result is null,ops:"+opsCount);
                    io_uring_cqe_seen(ring,cqe);
                   continue;
                }
                result.res = io_uring_cqe.res$get(cqe);
                result.bid = io_uring_cqe.flags$get(cqe) >> IORING_CQE_BUFFER_SHIFT();
                io_uring_cqe_seen(ring,cqe);
                results.add(result);
            }
            return results;
        }

    }


    /**
     * 等待一个事件
     * @return
     */
    public List<IOOpResult> waitFd(){
        //及时释放内存
        //太弱智了 不支持栈上分配
        try (MemorySession tmp = MemorySession.openConfined()) {
            MemorySegment ref = tmp.allocate(C_POINTER.byteSize());
            io_uring_wait_cqe(ring, ref);
            MemoryAddress cqePoint = ref.get(C_POINTER, 0);
            MemorySegment cqe = io_uring_cqe.ofAddress(cqePoint, MemorySession.global());
            long opeCount = io_uring_cqe.user_data$get(cqe);
            if (opeCount < 0){
                //非fd
                io_uring_cqe_seen(ring,cqe);
                return List.of();
            }
            IOOpResult result = context.remove(opeCount);
            result.res = io_uring_cqe.res$get(cqe);
            result.bid = io_uring_cqe.flags$get(cqe) >> IORING_CQE_BUFFER_SHIFT();
            io_uring_cqe_seen(ring,cqe);
            return List.of(result);
        }

    }
    public void waitComplete(){
        waitComplete(-1);
    }

    public void waitComplete(long waitMs){
        try (MemorySession memorySession = MemorySession.openConfined()) {
            MemorySegment cqe_ptr = memorySession.allocate(C_POINTER.byteSize());
            if (waitMs == -1){
                io_uring_wait_cqe(ring, cqe_ptr);
                return;
            }

            MemorySegment ts = __kernel_timespec.allocate(allocator);
            __kernel_timespec.tv_sec$set(ts,waitMs/1000);
            __kernel_timespec.tv_nsec$set(ts, (waitMs % 1000) * 1000000);
            io_uring_wait_cqe_timeout(ring,cqe_ptr,ts);
        }
    }


    @Override
    public void close() throws Exception {
        io_uring_queue_exit(ring);
        allocator.close();
    }

    static {
//        System.load("/home/dreamlike/uringDemo/src/main/resources/liburing.so");
        try {

            if (!NativeHelper.isLinux() || !NativeHelper.isX86_64() || !NativeHelper.compareWithCurrentLinuxVersion(5, 10)) {
                throw new NativeCallException("please check os version(need linux >= 5.10) and CPU Arch(need X86_64)");
            }


            InputStream is = IOUring.class.getResourceAsStream("/liburing.so");
            File file = File.createTempFile("liburing", ".so");
            OutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
            System.load(file.getAbsolutePath());
            file.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
