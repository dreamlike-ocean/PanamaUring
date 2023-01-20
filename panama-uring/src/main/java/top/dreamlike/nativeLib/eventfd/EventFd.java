package top.dreamlike.nativeLib.eventfd;

import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static java.lang.foreign.ValueLayout.JAVA_LONG;

public class EventFd implements AutoCloseable {

    private final int fd;


    public EventFd() {
        fd = NativeHelper.createEventFd();
    }

    public void write(int count){
        int res = eventfd_h.eventfd_write(fd, count);
        if (res < 0){
            throw new NativeCallException(NativeHelper.getNowError());
        }
    }

    @Unsafe("需要segment一直有效")
    public void read(MemorySegment segment) {
        MemorySession memorySession = segment.session();
        if (!memorySession.isAlive() || memorySession.ownerThread() != null) {
            throw new NativeCallException("illegal memory segment");
        }

        if (segment.byteSize() != JAVA_LONG.byteSize()) {
            throw new NativeCallException("segment.byteSize() != JAVA_LONG.byteSize()");
        }
        int res = eventfd_h.eventfd_read(fd, segment);
        if (res < 0) {
            throw new NativeCallException(NativeHelper.getNowError());
        }
    }

    public long read(){
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment tmp = session.allocate(JAVA_LONG, 0);
            read(tmp);
            return tmp.get(JAVA_LONG, 0);
        }
    }

    @Override
    public void close() throws Exception {
        unistd_h.close(fd);
    }

    public int getFd() {
        return fd;
    }
}
