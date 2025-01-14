package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

public class IoUringBufReg {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringBufReg.class);

    public static final VarHandle BGID_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bgid")).withInvokeExactBehavior();

    private long ring_addr;
    private int ring_ring_entries;
    private short bgid;
    private short flags;
    @NativeArrayMark(length = 3, size = long.class)
    private MemorySegment resv;

    public long getRing_addr() {
        return ring_addr;
    }

    public void setRing_addr(long ring_addr) {
        this.ring_addr = ring_addr;
    }

    public int getRing_ring_entries() {
        return ring_ring_entries;
    }

    public void setRing_ring_entries(int ring_ring_entries) {
        this.ring_ring_entries = ring_ring_entries;
    }

    public short getBgid() {
        return bgid;
    }

    public void setBgid(short bgid) {
        this.bgid = bgid;
    }

    public short getFlags() {
        return flags;
    }

    public void setFlags(short flags) {
        this.flags = flags;
    }

    public MemorySegment getResv() {
        return resv;
    }

    public void setResv(MemorySegment resv) {
        this.resv = resv;
    }
}
