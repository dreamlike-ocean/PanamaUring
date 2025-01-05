import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public class AllocatorTest {

    @Test
    public void testAlignment() {
        MemoryAllocator<OwnershipMemory> libcMalloc = MemoryAllocator.LIBC_MALLOC;

        try (Arena arena = libcMalloc.disposableArena()) {
            int byteAlignment = 4 * 1024;
            MemorySegment memorySegment = arena.allocate(100, byteAlignment);
            long address = memorySegment.address();
            Assert.assertEquals(0, address % byteAlignment);
            Assert.assertEquals(100, memorySegment.byteSize());
        }
    }

}
