package top.dreamlike.panama.uring.fd;

import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.trait.NativeFd;
import top.dreamlike.panama.uring.trait.PollableFd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class EventFd implements NativeFd, PollableFd {
    private final int fd;


    public EventFd(int init,int flags) {
        int eventfd = Instance.LIBC.eventfd(init, flags);
        if (eventfd < 0) {
            throw new IllegalArgumentException(STR."eventfd create failed, error: \{DebugHelper.currentErrorStr()}");
        }
        this.fd = eventfd;
    }

    public EventFd() {
        this(0, 0);
    }

    public int eventfdWrite(int value) {
        return Instance.LIBC.eventfd_write(fd, value);
    }

    @Override
    public int read(MemorySegment buf, int count) {
        if (buf.byteSize() < ValueLayout.JAVA_LONG.byteSize()) {
            throw new IllegalArgumentException("MemorySegment size is too small");
        }
        int len = 8;
        return Instance.LIBC.read(fd, buf, len);
    }

    public int eventfdRead(MemorySegment segment) {
        if (segment.byteSize() < ValueLayout.JAVA_LONG.byteSize()) {
            throw new IllegalArgumentException("MemorySegment size is too small");
        }
        return Instance.LIBC.eventfd_read(fd,segment);
    }

    @Override
    public int fd() {
        return fd;
    }

}
