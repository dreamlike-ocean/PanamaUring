package top.dreamlike.asyncFile;

import top.dreamlike.liburing.io_uring;
import top.dreamlike.liburing.io_uring_cqe;
import top.dreamlike.liburing.io_uring_sqe;
import top.dreamlike.liburing.iovec;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

import static top.dreamlike.liburing.liburing_h.*;

public class IOUring implements AutoCloseable{
    private final MemorySegment ring;
    final  MemorySession allocator;

    private static int max_loop = 1024;

    private Map<Integer,IOOpResult> context;

    public IOUring(int ringSize){
        if (ringSize <= 0) {
            throw new IllegalArgumentException("ring size <= 0");
        }
        context = new ConcurrentHashMap<>();
        allocator = MemorySession.openConfined();
        this.ring = io_uring.allocate(allocator);
        var ret= io_uring_queue_init(ringSize, ring, 0);
        if (ret < 0){
            throw new IllegalStateException("io_uring init fail!");
        }
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
        MemorySegment iovecStruct = iovec.allocate(allocator);
        iovec.iov_base$set(iovecStruct, buffer.address());
        iovec.iov_len$set(iovecStruct,buffer.byteSize());
        MemorySegment sqeSegment = MemorySegment.ofAddress(sqe, io_uring_sqe.sizeof(), MemorySession.global());
        io_uring_prep_readv(sqe,fd,iovecStruct, 1, offset);
        io_uring_sqe.user_data$set(sqeSegment,fd);
        context.put(fd, new IOOpResult(fd, -1,buffer,callback));
        return true;
    }


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



    public List<IOOpResult> waitFd(){
        MemorySegment ref = allocator.allocate(C_POINTER.byteSize());
        __io_uring_get_cqe(ring,ref,0,1,NULL());
        MemoryAddress memoryAddress = ref.get(C_POINTER, 0);
        MemorySegment cqe = io_uring_cqe.ofAddress(memoryAddress, MemorySession.global());
        long fd = io_uring_cqe.user_data$get(cqe);
        IOOpResult result = context.remove((int)fd);
        result.res = io_uring_cqe.res$get(cqe);
        return List.of(result);
    }

    @Override
    public void close() throws Exception {
        allocator.close();
    }
}
