package top.dreamlike.panama.uring.sync.trait;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemorySegment;

public interface ReadableNativeFd {

    int readFd();

    default int read(MemorySegment buf, int count) {
        int fd = readFd();
        int len = (int) Math.min(buf.byteSize(), count);
        return Instance.LIBC.read(fd, buf, len);
    }


}
