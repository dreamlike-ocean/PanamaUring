
package top.dreamlike.panama.uring.trait;

import java.lang.foreign.MemorySegment;

public interface OwnershipMemory {
    MemorySegment resource();
    
    void drop();

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
}