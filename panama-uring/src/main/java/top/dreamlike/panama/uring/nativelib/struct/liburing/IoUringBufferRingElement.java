package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;

import java.lang.foreign.MemorySegment;

public record IoUringBufferRingElement(IoUringBufferRing ring, int bid, MemorySegment element, boolean hasOccupy){
}
