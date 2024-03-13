package top.dreamlike.panama.nativelib.struct.sigset;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class SigsetType {
    @NativeArrayMark(size = long.class, length = 16)
    private MemorySegment __val;
}
