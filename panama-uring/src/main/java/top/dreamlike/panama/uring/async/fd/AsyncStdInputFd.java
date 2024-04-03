package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.fd.StdInputFd;

public class AsyncStdInputFd extends StdInputFd implements IoUringAsyncFd {
    private final IoUringEventLoop owner;

    public AsyncStdInputFd(IoUringEventLoop owner) {
        this.owner = owner;
    }

    @Override
    public IoUringEventLoop owner() {
        return owner;
    }
}
