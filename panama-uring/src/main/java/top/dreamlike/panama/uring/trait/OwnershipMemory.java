
package top.dreamlike.panama.uring.trait;

import top.dreamlike.panama.uring.async.BufferResult;

import java.lang.foreign.MemorySegment;

public interface OwnershipMemory extends OwnershipResource<MemorySegment> {
    MemorySegment resource();

    default <T> void DropWhenException(T t, Throwable ex) {
        if (ex != null) {
            drop();
        }
    }

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