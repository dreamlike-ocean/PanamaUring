package top.dreamlike.panama.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class IoUringBufReg {
    private long ring_addr;
    private int ring_ring_entries;
    private short bgid;
    private short flags;
    @NativeArrayMark(length = 3, size = long.class)
    private MemorySegment resv;
}
