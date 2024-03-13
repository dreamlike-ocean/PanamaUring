package top.dreamlike.panama.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class IoUringCqe {
    public long user_data;
    public int res;
    public int flags;
    @NativeArrayMark(size = long.class, length = 0)
    public MemorySegment big_cqe;
}
