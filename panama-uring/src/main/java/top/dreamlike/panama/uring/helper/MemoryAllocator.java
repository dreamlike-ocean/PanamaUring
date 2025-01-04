package top.dreamlike.panama.uring.helper;

import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public interface MemoryAllocator {
    OwnershipMemory allocateOwnerShipMemory(long size);
    default void free(OwnershipMemory memory) {
        memory.drop();
    }

    MemoryAllocator JDK_SINGLE_THREAD = new MemoryAllocator() {
        @Override
        public OwnershipMemory allocateOwnerShipMemory(long size) {
            Arena arena = Arena.ofConfined();
            MemorySegment memorySegment = arena.allocate(size);
            return new OwnershipMemory() {
                @Override
                public MemorySegment resource() {
                    return memorySegment;
                }

                @Override
                public void drop() {
                    arena.close();
                }
            };
        }
    };

    MemoryAllocator JDK_MULTI_THREAD = new MemoryAllocator() {
        @Override
        public OwnershipMemory allocateOwnerShipMemory(long size) {
            Arena arena = Arena.ofShared();
            MemorySegment memorySegment = arena.allocate(size);
            return new OwnershipMemory() {
                @Override
                public MemorySegment resource() {
                    return memorySegment;
                }

                @Override
                public void drop() {
                    arena.close();
                }
           };
        }
    };
}
