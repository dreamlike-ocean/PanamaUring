package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Skip;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class IoUring {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUring.class);

    private IoUringSq sq;
    private IoUringCq cq;
    private int flags;
    private int ring_fd;
    private int features;
    private int enter_ring_fd;
    private byte int_flags;
    @NativeArrayMark(size = byte.class, length = 3)
    private MemorySegment pad;
    private int pad2;

    @Skip
    private MemorySegment getEventsArgs;

    public IoUringSq getSq() {
        return sq;
    }

    public void setSq(IoUringSq sq) {
        this.sq = sq;
    }

    public IoUringCq getCq() {
        return cq;
    }

    public void setCq(IoUringCq cq) {
        this.cq = cq;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getRing_fd() {
        return ring_fd;
    }

    public void setRing_fd(int ring_fd) {
        this.ring_fd = ring_fd;
    }

    public int getFeatures() {
        return features;
    }

    public void setFeatures(int features) {
        this.features = features;
    }

    public int getEnter_ring_fd() {
        return enter_ring_fd;
    }

    public void setEnter_ring_fd(int enter_ring_fd) {
        this.enter_ring_fd = enter_ring_fd;
    }

    public MemorySegment getPad() {
        return pad;
    }

    public void setPad(MemorySegment pad) {
        this.pad = pad;
    }

    public int getPad2() {
        return pad2;
    }

    public void setPad2(int pad2) {
        this.pad2 = pad2;
    }

    public byte getInt_flags() {
        return int_flags;
    }

    public void setInt_flags(byte int_flags) {
        this.int_flags = int_flags;
    }

    public MemorySegment getGetEventsArgs() {
        return getEventsArgs;
    }

    public void setGetEventsArgs(MemorySegment getEventsArgs) {
        this.getEventsArgs = getEventsArgs;
    }
}
