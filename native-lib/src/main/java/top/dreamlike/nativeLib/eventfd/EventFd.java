package top.dreamlike.nativeLib.eventfd;

import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

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
    public void readSyncUnsafe(MemorySegment segment) {
        if (segment.byteSize() != JAVA_LONG.byteSize()) {
            throw new NativeCallException("segment.byteSize() != JAVA_LONG.byteSize()");
        }
        int res = eventfd_h.eventfd_read(fd, segment);
//        if (res < 0) {
//            throw new NativeCallException(res+":"+NativeHelper.getNowError());
//        }
    }

    public long readSync() {
        try (Arena session = Arena.openConfined()) {
            MemorySegment tmp = session.allocate(JAVA_LONG, 0);
            readSyncUnsafe(tmp);
            return tmp.get(JAVA_LONG, 0);
        }
    }

    public void transToNoBlock() {
        NativeHelper.makeNoBlocking(fd);
    }

    @Override
    public void close() throws Exception {
        unistd_h.close(fd);
    }

    public int getFd() {
        return fd;
    }
}
