package top.dreamlike.panama.uring.nativelib.struct.iovec;

import top.dreamlike.panama.generator.annotation.Pointer;

import java.lang.foreign.MemorySegment;

public class Iovec {
    @Pointer
    private MemorySegment iov_base;
    private long iov_len;

    public MemorySegment getIov_base() {
        return iov_base;
    }

    public void setIov_base(MemorySegment iov_base) {
        this.iov_base = iov_base;
    }

    public long getIov_len() {
        return iov_len;
    }

    public void setIov_len(long iov_len) {
        this.iov_len = iov_len;
    }
}
