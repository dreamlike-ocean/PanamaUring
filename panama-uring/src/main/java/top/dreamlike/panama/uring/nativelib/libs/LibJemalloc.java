package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;

import java.lang.foreign.MemorySegment;

@CLib("jemalloc-ffi.so")
public interface LibJemalloc {

    @NativeFunction(value = "ring_malloc", fast = true)
    MemorySegment malloc(long size);

    @NativeFunction(value = "ring_free", fast = true)
    void free(@Pointer MemorySegment ptr);

    @NativeFunction(value = "ring_malloc_usable_size", fast = true)
    long malloc_usable_size(@Pointer MemorySegment ptr);
}
