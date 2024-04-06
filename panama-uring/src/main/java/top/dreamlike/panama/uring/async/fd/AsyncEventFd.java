package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.sync.fd.EventFd;

public class AsyncEventFd extends EventFd implements IoUringAsyncFd {
    private IoUringEventLoop owner;

    public AsyncEventFd(IoUringEventLoop owner,int init, int flags) {
        super(init, flags);
        this.owner = owner;
    }

    public AsyncEventFd(IoUringEventLoop owner) {
        this(owner, 0, 0);
    }

    @Override
    public IoUringEventLoop owner() {
        return owner;
    }
}
