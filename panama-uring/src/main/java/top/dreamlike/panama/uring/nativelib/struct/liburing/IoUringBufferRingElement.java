package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;

public record IoUringBufferRingElement(IoUringBufferRing ring, int bid, OwnershipMemory element, boolean hasOccupy) implements OwnershipMemory {

    public void release() {
        ring.releaseBuffer(this);
    }

    @Override
    public MemorySegment resource() {
        return element.resource();
    }

    @Override
    public void drop() {
        release();
    }
}
