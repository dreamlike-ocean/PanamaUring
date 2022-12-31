package top.dreamlike.async.uring;

import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.nativeLib.eventfd.eventfd_h;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.liburing.*;
import top.dreamlike.nativeLib.socket.sockaddr;

import java.io.*;
import java.lang.foreign.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_NONBLOCK;
import static top.dreamlike.nativeLib.inet.inet_h.inet_ntoa;
import static top.dreamlike.nativeLib.liburing.liburing_h.*;
import static top.dreamlike.nativeLib.string.string_h.strlen;

public class IOUring implements AutoCloseable{
    private final MemorySegment ring;

    private int eventfd = -1;
    final  MemorySession allocator;

    private static final int max_loop = 1024;

    private final byte gid;

    private final Map<Long, IOOpResult> context;

    private final MemorySegment[] selectedBuffer;

    private final AtomicLong count;

    private final int ringSize;


    public IOUring(int ringSize){
        this(ringSize,16);
    }
    public IOUring(int ringSize,int autoBufferSize){
        if (ringSize <= 0) {
            throw new IllegalArgumentException("ring size <= 0");
        }
        context = new HashMap<>();
        allocator = MemorySession.openShared();
        this.ring = io_uring.allocate(allocator);
        var ret= io_uring_queue_init(ringSize, ring, 0);
        if (ret < 0){
            throw new IllegalStateException("io_uring init fail!");
        }
        this.ringSize = ringSize;
        gid =(byte) new Random(System.currentTimeMillis()).nextInt(100);
        selectedBuffer = new MemorySegment[autoBufferSize];
        for (int i = 0; i < selectedBuffer.length; i++) {
            selectedBuffer[i] = allocator.allocate(1024);
        }

        for (int i = 0; i < selectedBuffer.length; i++) {
            if (!provideBuffer(selectedBuffer[i],i,false)) {
                submit();
            }
        }
        submit();
        count = new AtomicLong();

       eventfd = eventfd_h.eventfd(0, 0);
        io_uring_register_eventfd(ring,eventfd);
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

    public AsyncFile openFile(String path, int ops){
        return new AsyncFile(path,this,ops);
    }

    public AsyncServerSocket openServer(String host,int port){
        return new AsyncServerSocket(this,host,port);
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
            io_uring_prep_readv(sqe,fd,iovecStruct, 1, offset);
            io_uring_sqe.flags$set(sqeSegment, 0L, (byte) IOSQE_BUFFER_SELECT());

            io_uring_sqe.buf_group$set(sqeSegment, gid);
            IOOpResult result = new IOOpResult(fd, -1, null, (res,bid) -> {
                if (res == -ENOBUFS()){
                    readBufferPromise.completeExceptionally(new Exception(String.format("gid: %d 不存在空余的内存了", gid)));
                    return;
                }
                MemorySegment buffer = selectedBuffer[bid];
                readBufferPromise.complete(buffer.asSlice(0,res).toArray(ValueLayout.JAVA_BYTE));
                provideBuffer(buffer, bid,true);
            });
            long opsCount = count.getAndIncrement();
            io_uring_sqe.user_data$set(sqeSegment,opsCount);
            context.put(opsCount, result);
        }

        return true;
    }

    public boolean prep_selected_recv(int socketFd, int length, CompletableFuture<byte[]> recvBufferPromise){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;

        io_uring_prep_recv(sqe,socketFd, MemoryAddress.NULL, length,0);

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_sqe.flags$set(sqeSegment, 0L, (byte) IOSQE_BUFFER_SELECT());
        io_uring_sqe.buf_group$set(sqeSegment, gid);
        long opsCount = count.getAndIncrement();
        IOOpResult result = new IOOpResult(socketFd, -1, null, (res,bid) -> {
            if (res == -ENOBUFS()){
                recvBufferPromise.completeExceptionally(new Exception(String.format("gid: %d 不存在空余的内存了", gid)));
                return;
            }
            MemorySegment buffer = selectedBuffer[bid];
            recvBufferPromise.complete(buffer.asSlice(0,res).toArray(ValueLayout.JAVA_BYTE));
            provideBuffer(buffer, bid,true);
        });

        io_uring_sqe.user_data$set(sqeSegment,opsCount);
        context.put(opsCount, result);

        return true;
    }

    public boolean prep_send(int socketFd, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        io_uring_prep_send(sqe,socketFd,buffer,buffer.byteSize(), 0);
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(socketFd, -1, buffer, (res, __) -> callback.accept(res)));
        return true;
    }


    public boolean prep_accept(int fd, Consumer<AsyncSocket> callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySession tmp = MemorySession.openShared();
        MemorySegment client_addr = sockaddr.allocate(tmp);
        MemorySegment client_addr_len = tmp.allocate(JAVA_INT,(int) sockaddr.sizeof());
        io_uring_prep_accept(sqe,fd,client_addr,client_addr_len,0);
        long opsCount = count.getAndIncrement();
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(fd, -1, null, (res, __) -> {
            if (res < 0){
                callback.accept(new AsyncSocket(res,"" , -1,this));
            }else {
                short port = sockaddr_in.sin_port$get(client_addr);
                MemoryAddress remoteHost = inet_ntoa(top.dreamlike.nativeLib.inet.sockaddr_in.sin_addr$slice(client_addr));
                long strlen = strlen(remoteHost);
                String host = new String(MemorySegment.ofAddress(remoteHost, strlen, MemorySession.global()).toArray(JAVA_BYTE));
                callback.accept(new AsyncSocket(res,host , port,this));
            }
            tmp.close();
        }));
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
            io_uring_prep_read(sqe,fd,buffer,(int) buffer.byteSize(), offset);
            long opsCount = count.getAndIncrement();
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(fd, -1, buffer, (res, __) -> callback.accept(res)));
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
            io_uring_prep_timeout(sqeSegment,ts,0,0);
            long opsCount = count.getAndIncrement();
            io_uring_sqe.user_data$set(sqeSegment, opsCount);
            context.put(opsCount, new IOOpResult(-1, -1, null, (res, __) -> runnable.run()));
        }
        return true;
    }

    protected MemoryAddress getSqe() {
        int i = 0;
        MemoryAddress sqe = null;
        while (sqe == null){
            sqe = io_uring_get_sqe(ring);
            i++;
            if (i == max_loop) return null;
        }
        return sqe;
    }
//sqe -> cqe

    public boolean prep_write(int fd, int offset, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;

        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_write(sqe, fd, buffer, (int) buffer.byteSize(), offset);

        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(fd, -1, buffer, (res, __) -> callback.accept(res)));
        return true;
    }

    public boolean prep_no_op(Runnable runnable){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_nop(sqeSegment);
        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(-1, -1, null, (res, __) -> runnable.run()));
        return true;
    }

    public boolean prep_fsync(int fd,int fsyncflag,Consumer<Integer> runnable){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_fsync(sqe, fd,fsyncflag);
        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment, opsCount);
        context.put(opsCount, new IOOpResult(-1, -1, null, (res, __) -> runnable.accept(res)));
        return true;
    }

    public boolean wakeup(){
        if (prep_no_op(() -> {})) {
            submit();
            return true;
        }
        return false;
    }

    public boolean provideBuffer(MemorySegment buffer,int bid,boolean needSubmit){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;

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
