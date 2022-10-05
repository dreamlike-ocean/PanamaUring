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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_NONBLOCK;
import static top.dreamlike.nativeLib.liburing.liburing_h.*;

public class IOUring implements AutoCloseable{
    private final MemorySegment ring;

    private int eventfd = -1;
    final  MemorySession allocator;

  private static final int max_loop = 1024;

    private final Map<Integer,IOOpResult> context;

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
    public boolean prep_readV(int fd, int offset, MemorySegment buffer, IntConsumer callback){
        int i = 0;
        MemoryAddress sqe = null;
        while (sqe == null){
            sqe = io_uring_get_sqe(ring);
            i++;
            if (i == max_loop) return false;
        }
        //byte*,len
        MemorySegment iovecStruct = iovec.allocate(allocator);
        iovec.iov_base$set(iovecStruct, buffer.address());
        iovec.iov_len$set(iovecStruct,buffer.byteSize());
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_readv(sqe,fd,iovecStruct, 1, offset);
        io_uring_sqe.user_data$set(sqeSegment,fd);
        context.put(fd, new IOOpResult(fd, -1,buffer,callback));
        return true;
    }
//sqe -> cqe

    public boolean prep_writeV(int fd,int offset,MemorySegment buffer,IntConsumer callback){
        int i = 0;
        MemoryAddress sqe = null;
        while (sqe == null){
            sqe = io_uring_get_sqe(ring);
            i++;
            if (i == max_loop) return false;
        }
        MemorySegment iovecStruct = iovec.allocate(allocator);
        iovec.iov_base$set(iovecStruct, buffer.address());
        iovec.iov_len$set(iovecStruct,buffer.byteSize());
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_writev(sqe,fd,iovecStruct, 1, offset);
        io_uring_sqe.user_data$set(sqeSegment,fd);
        context.put(fd, new IOOpResult(fd, -1,buffer,callback));
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
                long fd = io_uring_cqe.user_data$get(cqe);
                IOOpResult result = context.remove((int) fd);
                result.res = io_uring_cqe.res$get(cqe);
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
            IOOpResult result = context.remove((int) fd);
            result.res = io_uring_cqe.res$get(cqe);
            io_uring_cqe_seen(ring,cqe);
            return List.of(result);
        }

    }

    @Override
    public void close() throws Exception {
        io_uring_queue_exit(ring);
    }
}
