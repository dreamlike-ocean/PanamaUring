package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class IoUringParams {
    private int sq_entries;
    private int cq_entries;
    private int flags;
    private int sq_thread_cpu;
    private int sq_thread_idle;
    private int features;
    private int wq_fd;
    @NativeArrayMark(size = int.class, length = 3)
    private MemorySegment resv;

    private IoSqringOffsets sq_off;

    private IoCqruingOffsets cq_off;

    public int getSq_entries() {
        return sq_entries;
    }

    public void setSq_entries(int sq_entries) {
        this.sq_entries = sq_entries;
    }

    public int getCq_entries() {
        return cq_entries;
    }

    public void setCq_entries(int cq_entries) {
        this.cq_entries = cq_entries;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getSq_thread_cpu() {
        return sq_thread_cpu;
    }

    public void setSq_thread_cpu(int sq_thread_cpu) {
        this.sq_thread_cpu = sq_thread_cpu;
    }

    public int getSq_thread_idle() {
        return sq_thread_idle;
    }

    public void setSq_thread_idle(int sq_thread_idle) {
        this.sq_thread_idle = sq_thread_idle;
    }

    public int getFeatures() {
        return features;
    }

    public void setFeatures(int features) {
        this.features = features;
    }

    public int getWq_fd() {
        return wq_fd;
    }

    public void setWq_fd(int wq_fd) {
        this.wq_fd = wq_fd;
    }

    public MemorySegment getResv() {
        return resv;
    }

    public void setResv(MemorySegment resv) {
        this.resv = resv;
    }

    public IoSqringOffsets getSq_off() {
        return sq_off;
    }

    public void setSq_off(IoSqringOffsets sq_off) {
        this.sq_off = sq_off;
    }

    public IoCqruingOffsets getCq_off() {
        return cq_off;
    }

    public void setCq_off(IoCqruingOffsets cq_off) {
        this.cq_off = cq_off;
    }

    public static class IoSqringOffsets {
        private int head;
        private int tail;
        private int ring_mask;
        private int ring_entries;
        private int flags;
        private int dropped;
        private int array;
        private int resv1;
        private long user_addr;

        public int getHead() {
            return head;
        }

        public void setHead(int head) {
            this.head = head;
        }

        public int getTail() {
            return tail;
        }

        public void setTail(int tail) {
            this.tail = tail;
        }

        public int getRing_mask() {
            return ring_mask;
        }

        public void setRing_mask(int ring_mask) {
            this.ring_mask = ring_mask;
        }

        public int getRing_entries() {
            return ring_entries;
        }

        public void setRing_entries(int ring_entries) {
            this.ring_entries = ring_entries;
        }

        public int getFlags() {
            return flags;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public int getDropped() {
            return dropped;
        }

        public void setDropped(int dropped) {
            this.dropped = dropped;
        }

        public int getArray() {
            return array;
        }

        public void setArray(int array) {
            this.array = array;
        }

        public int getResv1() {
            return resv1;
        }

        public void setResv1(int resv1) {
            this.resv1 = resv1;
        }

        public long getUser_addr() {
            return user_addr;
        }

        public void setUser_addr(long user_addr) {
            this.user_addr = user_addr;
        }
    }

    public static class IoCqruingOffsets {
        private int head;
        private int tail;
        private int ring_mask;
        private int ring_entries;
        private int overflow;
        private int cqes;
        private int flags;
        private int resv1;
        private long user_addr;

        public int getHead() {
            return head;
        }

        public void setHead(int head) {
            this.head = head;
        }

        public int getTail() {
            return tail;
        }

        public void setTail(int tail) {
            this.tail = tail;
        }

        public int getRing_mask() {
            return ring_mask;
        }

        public void setRing_mask(int ring_mask) {
            this.ring_mask = ring_mask;
        }

        public int getRing_entries() {
            return ring_entries;
        }

        public void setRing_entries(int ring_entries) {
            this.ring_entries = ring_entries;
        }

        public int getOverflow() {
            return overflow;
        }

        public void setOverflow(int overflow) {
            this.overflow = overflow;
        }

        public int getCqes() {
            return cqes;
        }

        public void setCqes(int cqes) {
            this.cqes = cqes;
        }

        public int getFlags() {
            return flags;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public int getResv1() {
            return resv1;
        }

        public void setResv1(int resv) {
            this.resv1 = resv;
        }

        public long getUser_addr() {
            return user_addr;
        }

        public void setUser_addr(long user_addr) {
            this.user_addr = user_addr;
        }
    }
}
