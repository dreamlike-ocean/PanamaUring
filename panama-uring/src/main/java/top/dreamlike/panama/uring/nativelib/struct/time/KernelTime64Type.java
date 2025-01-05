package top.dreamlike.panama.uring.nativelib.struct.time;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;

public class KernelTime64Type {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(KernelTime64Type.class);

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
