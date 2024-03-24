package top.dreamlike.panama.uring.trait;

import top.dreamlike.panama.uring.nativelib.Instance;

public interface NativeFd extends ReadableNativeFd, WritableNativeFd {
    int fd();

    @Override
    default int writeFd() {
        return fd();
    }

    @Override
    default int readFd() {
        return fd();
    }

    default void close() {
        Instance.LIBC.close(fd());
    }
}
