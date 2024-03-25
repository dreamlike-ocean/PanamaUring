package top.dreamlike.panama.uring.trait;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

public interface IoUringAsyncFd extends NativeFd {
    IoUringEventLoop owner();

}
