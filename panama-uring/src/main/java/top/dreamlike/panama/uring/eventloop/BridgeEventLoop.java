package top.dreamlike.panama.uring.eventloop;

import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.util.function.Consumer;

public non-sealed class BridgeEventLoop extends IoUringEventLoop {

    public BridgeEventLoop(Thread currentEventLoopThread, Consumer<IoUringParams> ioUringParamsFactory) {
        super(ioUringParamsFactory, r -> currentEventLoopThread);
    }

    @Override
    public void start() {
        //do nothing
    }

    @Override
    public void close() throws Exception {
        if (hasClosed.compareAndSet(false, true)) {
            runOnEventLoop(this::releaseResource);
        }
    }

    @Override
    protected boolean needWakeUpFd() {
        return false;
    }
}

