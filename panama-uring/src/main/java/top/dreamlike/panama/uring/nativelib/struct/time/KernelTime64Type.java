package top.dreamlike.panama.uring.nativelib.struct.time;

public class KernelTime64Type {
    private long tv_sec;
    private long tv_nsec;

    public long getTv_nsec() {
        return tv_nsec;
    }

    public void setTv_nsec(long tv_nsec) {
        this.tv_nsec = tv_nsec;
    }

    public long getTv_sec() {
        return tv_sec;
    }

    public void setTv_sec(long tv_sec) {
        this.tv_sec = tv_sec;
    }
}
