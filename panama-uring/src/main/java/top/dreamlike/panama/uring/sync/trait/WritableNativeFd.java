package top.dreamlike.panama.uring.sync.trait;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemorySegment;

public interface WritableNativeFd {
    int writeFd();

    default int write(MemorySegment buf, int count) {
        int fd = writeFd();
        int len = (int) Math.min(buf.byteSize(), count);
        return Instance.LIBC.write(fd, buf, len);
    }
}
