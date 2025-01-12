package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.invoke.VarHandle;

public class IoUringGetEventsArg {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringGetEventsArg.class);

    public static final VarHandle SIGMASK_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("sigmask"));

    public static final VarHandle SIGMASK_SZ_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("sigmasksz"));

    public static final VarHandle MIN_WAIT_USECSEC_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("minWaitUsecsec"));

    public static final VarHandle TS_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ts"));

    private long sigmask;
    private int sigmasksz;
    private int minWaitUsecsec;
    private long ts;

    public long getSigmask() {
        return sigmask;
    }

    public void setSigmask(long sigmask) {
        this.sigmask = sigmask;
    }

    public int getSigmasksz() {
        return sigmasksz;
    }

    public void setSigmasksz(int sigmasksz) {
        this.sigmasksz = sigmasksz;
    }

    public int getMinWaitUsecsec() {
        return minWaitUsecsec;
    }

    public void setMinWaitUsecsec(int minWaitUsecsec) {
        this.minWaitUsecsec = minWaitUsecsec;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
}
