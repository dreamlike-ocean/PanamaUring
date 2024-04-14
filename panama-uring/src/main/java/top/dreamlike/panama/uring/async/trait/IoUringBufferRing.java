package top.dreamlike.panama.uring.async.trait;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;

import java.util.concurrent.CompletableFuture;

public interface IoUringBufferRing {

    public IoUringBufferRingElement getMemoryByBid(int bid);

    public CompletableFuture<IoUringBufferRingElement> removeBuffer(int bid);

    public CompletableFuture<Void> releaseBuffer(IoUringBufferRingElement element);

    public short getBufferGroupId();

    public CompletableFuture<Void> releaseRing();

    public boolean hasAvailableElements();

    public IoUringEventLoop owner();
}
