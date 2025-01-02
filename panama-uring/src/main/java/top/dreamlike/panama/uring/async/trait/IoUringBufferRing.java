package top.dreamlike.panama.uring.async.trait;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;

import java.util.concurrent.CompletableFuture;

public interface IoUringBufferRing {

    IoUringBufferRingElement getMemoryByBid(int bid);

    CompletableFuture<IoUringBufferRingElement> removeBuffer(int bid);

    CompletableFuture<Void> releaseBuffer(IoUringBufferRingElement element);

    short getBufferGroupId();

    CompletableFuture<Void> releaseRing();

    boolean hasAvailableElements();

    IoUringEventLoop owner();

    void fillSqe(IoUringSqe sqe);
}
