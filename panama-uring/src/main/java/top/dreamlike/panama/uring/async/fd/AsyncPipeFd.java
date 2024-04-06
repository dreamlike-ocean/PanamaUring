package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.sync.fd.PipeFd;

public class AsyncPipeFd extends PipeFd implements IoUringOperator, IoUringAsyncFd {

    private final IoUringEventLoop driver;

    public AsyncPipeFd(IoUringEventLoop driver) {
        this.driver = driver;
    }


    @Override
    public IoUringEventLoop owner() {
        return driver;
    }
}
