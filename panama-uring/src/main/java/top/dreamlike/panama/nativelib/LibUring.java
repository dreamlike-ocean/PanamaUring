package top.dreamlike.panama.nativelib;

import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.nativelib.struct.liburing.IoUringParams;

import java.lang.foreign.MemorySegment;

public interface LibUring {
    int io_uring_queue_init(int entries, @Pointer IoUring ring, int flags);
    int io_uring_queue_init_params(int entries, @Pointer IoUring ring, @Pointer IoUringParams p);
    void io_uring_queue_exit(@Pointer IoUring ring);
    @NativeFunction(fast = true)
    int io_uring_peek_batch_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqes, int count);
    @NativeFunction(fast = true)
    int io_uring_peek_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr);

}
