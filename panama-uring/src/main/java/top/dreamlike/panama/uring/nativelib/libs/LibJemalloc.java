package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

@CLib("jemalloc-ffi.so")
public interface LibJemalloc{

    @NativeFunction(value = "ring_malloc", fast = true)
    MemorySegment malloc(long size);

    @NativeFunction(value = "ring_free", fast = true)
    void free(@Pointer MemorySegment ptr);

    @NativeFunction(value = "ring_malloc_usable_size", fast = true)
    long malloc_usable_size(@Pointer MemorySegment ptr);

    @NativeFunction(value = "ring_posix_memalign", fast = true)
    int posix_memalign(@Pointer MemorySegment memptr, long alignment, long size);

    default OwnershipMemory mallocMemory(long size) {
        MemorySegment memorySegment = malloc(size);
        return new OwnershipMemory() {
            @Override
            public MemorySegment resource() {
                return memorySegment;
            }

            @Override
            public void drop() {
                free(memorySegment);
            }
        };
    }

    default OwnershipMemory posixMemalign(@Pointer MemorySegment memptr, long alignment, long size) {
        int ret = posix_memalign(memptr, alignment, size);
        if (ret != 0) {
            throw new RuntimeException("posix_memalign failed with error code: " + ret);
        }
        MemorySegment bases = memptr.get(ValueLayout.ADDRESS, 0);
        return new OwnershipMemory() {
            @Override
            public MemorySegment resource() {
                return bases;
            }

            @Override
            public void drop() {
                free(bases);
            }
        };
    }
}
