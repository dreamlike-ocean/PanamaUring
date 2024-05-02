package top.dreamlike.panama.uring.networking.eventloop;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.util.function.Consumer;

public class IoUringEventLoopOption<T extends IoUringEventLoopOption<T>> {
    protected int ringSize = 32;
    protected int submissionQueueSize = 32;
    protected int completionQueueSize = 32;
    protected int flags = 0;

    protected int wqFd = -1;

    public int getRingSize() {
        return ringSize;
    }

    public T setRingSize(int ringSize) {
        this.ringSize = ringSize;
        return (T)this;
    }

    public int getSubmissionQueueSize() {
        return submissionQueueSize;
    }

    public T setSubmissionQueueSize(int submissionQueueSize) {
        this.submissionQueueSize = submissionQueueSize;
        return (T)this;
    }

    public int getCompletionQueueSize() {
        return completionQueueSize;
    }

    public T setCompletionQueueSize(int completionQueueSize) {
        this.completionQueueSize = completionQueueSize;
        return (T)this;
    }

    public int getFlags() {
        return flags;
    }

    public T setFlags(int flags) {
        this.flags = flags;
        return (T)this;
    }

    public int getWqFd() {
        return wqFd;
    }
    
    public T setWqFd(IoUringEventLoop ioUringEventLoop) {
        this.wqFd = PanamaUringSecret.findUring.apply(ioUringEventLoop).getRing_fd();
        this.flags = this.flags | IoUringConstant.IORING_SETUP_ATTACH_WQ;
        return (T)this;
    }

    Consumer<IoUringParams> toConfig() {
        return ioUringParams -> {
            ioUringParams.setSq_entries(submissionQueueSize);
            ioUringParams.setCq_entries(completionQueueSize);
            ioUringParams.setFlags(flags);
            if (wqFd != -1) {
                ioUringParams.setWq_fd(wqFd);
            }
        };
    }
}
