package top.dreamlike.asyncFile;

import top.dreamlike.epoll.Epoll;
import top.dreamlike.nativeLib.eventfd.eventfd_h;
import top.dreamlike.nativeLib.liburing.io_uring;
import top.dreamlike.nativeLib.liburing.io_uring_cqe;
import top.dreamlike.nativeLib.liburing.io_uring_sqe;
import top.dreamlike.nativeLib.liburing.iovec;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_NONBLOCK;
import static top.dreamlike.nativeLib.liburing.liburing_h.*;

public class IOUring implements AutoCloseable{
    private final MemorySegment ring;

    private int eventfd = -1;
    final  MemorySession allocator;

    private static final int max_loop = 1024;

    private final byte gid;

    private final Map<Long,IOOpResult> context;

    private final MemorySegment[] selectedBuffer;

    private final AtomicLong count;

    public IOUring(int ringSize){
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
        gid =(byte) new Random(System.currentTimeMillis()).nextInt(100);
        //TODO 写死了 稍后一改
        selectedBuffer = new MemorySegment[4];
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

    public AsyncFile openFile(String path,int ops){
        return new AsyncFile(path,this,ops);
    }

    public boolean checkSupport(int op){
        return io_uring_opcode_supported_ring(ring, op) == 1;
    }


    public boolean selectedRead(int fd, int offset, int length, CompletableFuture<byte[]> callback){
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
                    callback.completeExceptionally(new Exception(String.format("gid: %d 不存在空余的内存了", gid)));
                    return;
                }
                MemorySegment buffer = selectedBuffer[bid];
                callback.complete(buffer.asSlice(0,res).toArray(ValueLayout.JAVA_BYTE));
                provideBuffer(buffer, bid,true);
            });
            long opsCount = count.getAndIncrement();
            io_uring_sqe.user_data$set(sqeSegment,opsCount);
            context.put(opsCount, result);
        }

        return true;
    }

    public boolean prep_readV(int fd, int offset, MemorySegment buffer, IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        //byte*,len
        MemorySegment iovecStruct = iovec.allocate(allocator);
        iovec.iov_base$set(iovecStruct, buffer.address());
        iovec.iov_len$set(iovecStruct,buffer.byteSize());
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_readv(sqe,fd,iovecStruct, 1, offset);

        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment,opsCount);
        context.put(opsCount, new IOOpResult(fd, -1,buffer,(res, __) -> callback.accept(res)));
        return true;
    }

    private MemoryAddress getSqe() {
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

    public boolean prep_writeV(int fd,int offset,MemorySegment buffer,IntConsumer callback){
        MemoryAddress sqe = getSqe();
        if (sqe == null) return false;
        MemorySegment iovecStruct = iovec.allocate(allocator);
        iovec.iov_base$set(iovecStruct, buffer.address());
        iovec.iov_len$set(iovecStruct,buffer.byteSize());
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_writev(sqe,fd,iovecStruct, 1, offset);

        long opsCount = count.getAndIncrement();
        io_uring_sqe.user_data$set(sqeSegment,opsCount);
        context.put(opsCount, new IOOpResult(fd, -1,buffer,(res, __) -> callback.accept(res)));
        return true;
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


    public List<IOOpResult> peekCqe(int max){
        try (MemorySession tmp = MemorySession.openConfined()) {
            MemorySegment ref = tmp.allocate(C_POINTER.byteSize());
            int count = 0;
            ArrayList<IOOpResult> results = new ArrayList<>();
            while (count <= max && io_uring_peek_cqe(ring, ref) == 0) {
                MemoryAddress cqePoint = ref.get(C_POINTER, 0);
                MemorySegment cqe = io_uring_cqe.ofAddress(cqePoint, MemorySession.global());
                long opsCount = io_uring_cqe.user_data$get(cqe);
                IOOpResult result = context.remove(opsCount);
                result.res = io_uring_cqe.res$get(cqe);
                result.bid = io_uring_cqe.flags$get(cqe) >> IORING_CQE_BUFFER_SHIFT();
                io_uring_cqe_seen(ring,cqe);
                results.add(result);
            }
            return results;
        }

    }


    public List<IOOpResult> waitFd(){
        //及时释放内存
        //太弱智了 不支持栈上分配
        try (MemorySession tmp = MemorySession.openConfined()) {
            MemorySegment ref = tmp.allocate(C_POINTER.byteSize());
            io_uring_wait_cqe(ring, ref);
            MemoryAddress cqePoint = ref.get(C_POINTER, 0);
            MemorySegment cqe = io_uring_cqe.ofAddress(cqePoint, MemorySession.global());
            long fd = io_uring_cqe.user_data$get(cqe);
            if (fd < 0){
                //非fd
                io_uring_cqe_seen(ring,cqe);
                return List.of();
            }
            IOOpResult result = context.remove(fd);
            result.res = io_uring_cqe.res$get(cqe);
            result.bid = io_uring_cqe.flags$get(cqe) >> IORING_CQE_BUFFER_SHIFT();
            io_uring_cqe_seen(ring,cqe);
            return List.of(result);
        }

    }

    @Override
    public void close() throws Exception {
        io_uring_queue_exit(ring);
    }
}
