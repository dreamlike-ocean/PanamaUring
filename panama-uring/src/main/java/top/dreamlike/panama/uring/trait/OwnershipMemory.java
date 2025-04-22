
package top.dreamlike.panama.uring.trait;

import java.lang.foreign.MemorySegment;

public interface OwnershipMemory extends OwnershipResource<MemorySegment> {
    MemorySegment resource();

    static OwnershipMemory of(MemorySegment segment) {
        return new OwnershipMemory() {
            @Override
            public MemorySegment resource() {
                return segment;
            }
            @Override
            public void drop() {

            }
        };
    }

    default OwnershipMemory slice(long offset, long newLength) {
        OwnershipMemory internalMemory = this;
        MemorySegment memorySegment = internalMemory.resource().asSlice(offset, newLength);
        return new OwnershipMemory() {
            @Override
            public MemorySegment resource() {
                return memorySegment;
            }

            @Override
            public void drop() {
                internalMemory.drop();
            }
        };
    }
}