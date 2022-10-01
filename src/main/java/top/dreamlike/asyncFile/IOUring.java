package top.dreamlike.asyncFile;

import top.dreamlike.liburing.io_uring;
import top.dreamlike.liburing.io_uring_cqe;
import top.dreamlike.liburing.io_uring_sqe;
import top.dreamlike.liburing.iovec;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static top.dreamlike.liburing.liburing_h.*;

public class IOUring implements AutoCloseable{
    private static final AtomicInteger count = new AtomicInteger();

    private final MemorySegment ring;
    final  MemorySession allocator;

    private final AtomicBoolean close = new AtomicBoolean(false);

    private final Thread carrierThread;

    private static final int max_loop = 1024;

    private final Map<Integer,IOOpResult> context;


    private final SignalPipe signalPipe;



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

        signalPipe = new SignalPipe(this);
        prep_readV(signalPipe.readFd, 0,signalPipe.signalBuffer,null);
        submit();
        carrierThread = new Thread(()->{
          while (!close.get()){
              for (IOOpResult result : waitFd()) {
                  if (result.callback == null) continue;
                  result.callback.accept(result.res);
              }
          }
            io_uring_queue_exit(ring);
            allocator.close();
            signalPipe.close();
        });
        carrierThread.setName("io_uring_"+count.getAndIncrement());
        carrierThread.start();

    }

    public void wake(){
        prep_readV(signalPipe.readFd, 0,signalPipe.signalBuffer,null);
        signalPipe.signal();
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
        submit();
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
        submit();
        context.put(fd, new IOOpResult(fd, -1,buffer,callback));
        return true;
    }


    public int submit(){
        return io_uring_submit(ring);
    }



    public List<IOOpResult> waitFd(){
        MemorySegment ref = allocator.allocate(C_POINTER.byteSize());
        io_uring_wait_cqe(ring, ref);
        MemoryAddress cqePoint = ref.get(C_POINTER, 0);
        MemorySegment cqe = io_uring_cqe.ofAddress(cqePoint, MemorySession.global());
        long fd = io_uring_cqe.user_data$get(cqe);
        IOOpResult result = context.remove((int) fd);
        result.res = io_uring_cqe.res$get(cqe);
        io_uring_cqe_seen(ring,cqe);
        return List.of(result);
    }

    @Override
    public void close() throws Exception {
        if (close.compareAndSet(false, true)) {
            wake();
        }
    }
}
