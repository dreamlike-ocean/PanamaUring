package top.dreamlike.panama.uring.async.trait;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.util.concurrent.CompletableFuture;

public interface IoUringBufferRing {

    OwnershipMemory removeBuffer(int bid);

    short getBufferGroupId();

    CompletableFuture<Void> releaseRing();

    IoUringEventLoop owner();

    void fillSqe(IoUringSqe sqe);

    int head();

    CompletableFuture<Integer> fillAll();

    void setAutoFill(boolean autoFill);
}
