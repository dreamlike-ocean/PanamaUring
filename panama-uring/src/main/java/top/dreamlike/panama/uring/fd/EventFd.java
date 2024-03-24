package top.dreamlike.panama.uring.fd;

import top.dreamlike.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.trait.NativeFd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class EventFd implements NativeFd {
    private final int fd;


    public EventFd(int init,int flags) {
        int eventfd = Instance.LIBC.eventfd(init, flags);
        if (eventfd < 0) {
            throw new IllegalArgumentException(STR."eventfd create failed, error: \{DebugHelper.currentErrorStr()}");
        }
        this.fd = eventfd;
    }

    public int eventfdWrite(int value) {
        return Instance.LIBC.eventfd_write(fd, value);
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
