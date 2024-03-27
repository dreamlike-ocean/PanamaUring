package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Union;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemorySegment;

@Union
public class IoUringBufRing {
    private AnonStruct anonStruct;

    @NativeArrayMark(size = IoUringBuf.class, length = 0)
    private MemorySegment bufs;

    public AnonStruct getAnonStruct() {
        return anonStruct;
    }

    public void setAnonStruct(AnonStruct anonStruct) {
        this.anonStruct = anonStruct;
    }

    public MemorySegment getBufs() {
        return bufs;
    }

    public void setBufs(MemorySegment bufs) {
        this.bufs = bufs;
    }

    public static class AnonStruct {
        private long resv1;
        private int resv2;
        private short resv3;
        private short tail;

        public long getResv1() {
            return resv1;
        }

        public void setResv1(long resv1) {
            this.resv1 = resv1;
        }

        public int getResv2() {
            return resv2;
        }

        public void setResv2(int resv2) {
            this.resv2 = resv2;
        }

        public short getResv3() {
            return resv3;
        }

        public void setResv3(short resv3) {
            this.resv3 = resv3;
        }

        public short getTail() {
            return tail;
        }

        public void setTail(short tail) {
            this.tail = tail;
        }
    }


    public void ioUringBufRingAdd(MemorySegment addr, int len,
                                         short bid, int mask, int bufOffset) {
        Instance.LIB_URING.io_uring_buf_ring_add(this, addr, len, bid, mask, bufOffset);
    }

    public void ioUringBufRingAdvance(int count) {
        Instance.LIB_URING.io_uring_buf_ring_advance(this, count);
    }
}
