package top.dreamlike.panama.uring.nativelib.struct.epoll;

import top.dreamlike.panama.generator.annotation.Alignment;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.annotation.Union;

import java.lang.foreign.MemorySegment;

@Alignment(byteSize = 1)
@Union
public  class EpollData {
    @Pointer
    private MemorySegment ptr;

    private int fd;

    private int u32;

    private long u64;

    public MemorySegment getPtr() {
        return ptr;
    }

    public void setPtr(MemorySegment ptr) {
        this.ptr = ptr;
    }

    public int getFd() {
        return fd;
    }

    public void setFd(int fd) {
        this.fd = fd;
    }

    public int getU32() {
        return u32;
    }

    public void setU32(int u32) {
        this.u32 = u32;
    }

    public long getU64() {
        return u64;
    }

    public void setU64(long u64) {
        this.u64 = u64;
    }
}

