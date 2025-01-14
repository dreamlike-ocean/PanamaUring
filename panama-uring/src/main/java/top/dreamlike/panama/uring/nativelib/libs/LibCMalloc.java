package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.NativeFunction;

import java.lang.foreign.MemorySegment;

public interface LibCMalloc {

    default MemorySegment mallocNoInit(long size) {
        return nativeMalloc(size)
                .reinterpret(size);
    }

    @NativeFunction("malloc")
    MemorySegment nativeMalloc(long size);

    void free(MemorySegment ptr);

    int malloc_trim(long pad);

    default MemorySegment malloc(long size) {
        return nativeMalloc(size)
                .reinterpret(size)
                .fill((byte) 0);
    }
}
