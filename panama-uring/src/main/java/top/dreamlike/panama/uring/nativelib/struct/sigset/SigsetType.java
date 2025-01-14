package top.dreamlike.panama.uring.nativelib.struct.sigset;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class SigsetType {

    public static final int _NSIG = 65;

    @NativeArrayMark(size = long.class, length = 16)
    private MemorySegment __val;
}
