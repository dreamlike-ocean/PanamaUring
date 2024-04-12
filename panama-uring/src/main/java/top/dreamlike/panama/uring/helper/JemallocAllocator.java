package top.dreamlike.panama.uring.helper;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;

public class JemallocAllocator implements SegmentAllocator {
    private final static int pageSize = Instance.LIBC.getpagesize();
    private static final long MAX_MALLOC_ALIGN = ValueLayout.ADDRESS.byteSize() == 4 ? 8 : 16;

    public static JemallocAllocator INSTANCE = new JemallocAllocator();

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        long alignedSize = Math.max(1L, byteAlignment > MAX_MALLOC_ALIGN ?
                byteSize + (byteAlignment - 1) :
                byteSize);
        return Instance.LIB_JEMALLOC.malloc(alignedSize);
    }

    public void free(MemorySegment segment) {
        Instance.LIB_JEMALLOC.free(segment);
    }
}
