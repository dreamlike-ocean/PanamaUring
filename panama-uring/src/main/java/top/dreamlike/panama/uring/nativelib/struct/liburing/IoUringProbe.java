package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Skip;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class IoUringProbe {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringProbe.class);

    public static final long OPS_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("ops"));

    private byte lastOp;

    private byte opsLen;

    private short resv;

    @NativeArrayMark(size = int.class, length = 3)
    private MemorySegment resv2;

    @NativeArrayMark(size = IoUringProbeOp.class, length = 0)
    private MemorySegment ops;

    @Skip
    private int features;

    public byte getLastOp() {
        return lastOp;
    }

    public void setLastOp(byte lastOp) {
        this.lastOp = lastOp;
    }

    public byte getOpsLen() {
        return opsLen;
    }

    public void setOpsLen(byte opsLen) {
        this.opsLen = opsLen;
    }

    public short getResv() {
        return resv;
    }

    public void setResv(short resv) {
        this.resv = resv;
    }

    public MemorySegment getResv2() {
        return resv2;
    }

    public void setResv2(MemorySegment resv2) {
        this.resv2 = resv2;
    }

    public MemorySegment getOps() {
        return ops;
    }

    public void setOps(MemorySegment ops) {
        this.ops = ops;
    }

    public int getFeatures() {
        return features;
    }

    public void setFeatures(int features) {
        this.features = features;
    }
}
