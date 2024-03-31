
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
}