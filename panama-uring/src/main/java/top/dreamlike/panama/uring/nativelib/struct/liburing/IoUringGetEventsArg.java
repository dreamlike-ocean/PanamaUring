package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;

public class IoUringGetEventsArg {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringGetEventsArg.class);

    private long sigmask;
    private int sigmask_sz;
    private int pad;
    private long ts;

    public long getSigmask() {
        return sigmask;
    }

    public void setSigmask(long sigmask) {
        this.sigmask = sigmask;
    }

    public int getSigmask_sz() {
        return sigmask_sz;
    }

    public void setSigmask_sz(int sigmask_sz) {
        this.sigmask_sz = sigmask_sz;
    }

    public int getPad() {
        return pad;
    }

    public void setPad(int pad) {
        this.pad = pad;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
}
