package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.fd.EventFd;
import top.dreamlike.panama.uring.trait.IoUringAsyncFd;

public class AsyncEventFd extends EventFd implements IoUringAsyncFd {
    private IoUringEventLoop owner;

    public AsyncEventFd(IoUringEventLoop owner,int init, int flags) {
        super(init, flags);
    }



    @Override
    public IoUringEventLoop owner() {
        return owner;
    }
}
