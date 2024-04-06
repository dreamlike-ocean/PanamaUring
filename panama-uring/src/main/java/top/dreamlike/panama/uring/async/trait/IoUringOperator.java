package top.dreamlike.panama.uring.async.trait;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

public interface IoUringOperator {
    IoUringEventLoop owner();
}
