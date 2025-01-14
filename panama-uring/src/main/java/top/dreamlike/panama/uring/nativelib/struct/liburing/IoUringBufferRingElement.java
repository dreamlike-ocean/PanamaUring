package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public record IoUringBufferRingElement(IoUringBufferRing ring, int bid, OwnershipMemory element, boolean hasOccupy) implements OwnershipMemory {

    public void release() {
        CompletableFuture<Void> asyncFuture = ring.releaseBuffer(this);
        IoUringEventLoop ioUringEventLoop = ring.owner();
        if (!ioUringEventLoop.inEventLoop()) {
            try {
                asyncFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
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
