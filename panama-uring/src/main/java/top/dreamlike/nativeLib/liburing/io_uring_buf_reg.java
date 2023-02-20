package top.dreamlike.nativeLib.liburing;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

public class io_uring_buf_reg {

    static final GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
            Constants$root.C_LONG_LONG$LAYOUT.withName("ring_addr"),
            Constants$root.C_INT$LAYOUT.withName("ring_entries"),
            Constants$root.C_SHORT$LAYOUT.withName("bgid"),
            Constants$root.C_SHORT$LAYOUT.withName("pad"),
            MemoryLayout.sequenceLayout(3, Constants$root.C_LONG_LONG$LAYOUT).withName("resv")
    ).withName("io_uring_buf_reg");

    public static MemoryLayout $LAYOUT() {
        return io_uring_buf_reg.$struct$LAYOUT;
    }

    static final VarHandle ring_addr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ring_addr"));

    public static VarHandle ring_addr$VH() {
        return io_uring_buf_reg.ring_addr$VH;
    }

    public static long ring_addr$get(MemorySegment seg) {
        return (long) io_uring_buf_reg.ring_addr$VH.get(seg);
    }

    public static void ring_addr$set(MemorySegment seg, long x) {
        io_uring_buf_reg.ring_addr$VH.set(seg, x);
    }

    public static long ring_addr$get(MemorySegment seg, long index) {
        return (long) io_uring_buf_reg.ring_addr$VH.get(seg.asSlice(index * sizeof()));
    }

    public static void ring_addr$set(MemorySegment seg, long index, long x) {
        io_uring_buf_reg.ring_addr$VH.set(seg.asSlice(index * sizeof()), x);
    }

    static final VarHandle ring_entries$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ring_entries"));

    public static VarHandle ring_entries$VH() {
        return io_uring_buf_reg.ring_entries$VH;
    }

    public static int ring_entries$get(MemorySegment seg) {
        return (int) io_uring_buf_reg.ring_entries$VH.get(seg);
    }

    public static void ring_entries$set(MemorySegment seg, int x) {
        io_uring_buf_reg.ring_entries$VH.set(seg, x);
    }

    public static int ring_entries$get(MemorySegment seg, long index) {
        return (int) io_uring_buf_reg.ring_entries$VH.get(seg.asSlice(index * sizeof()));
    }

    public static void ring_entries$set(MemorySegment seg, long index, int x) {
        io_uring_buf_reg.ring_entries$VH.set(seg.asSlice(index * sizeof()), x);
    }

    static final VarHandle bgid$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bgid"));

    public static VarHandle bgid$VH() {
        return io_uring_buf_reg.bgid$VH;
    }

    public static short bgid$get(MemorySegment seg) {
        return (short) io_uring_buf_reg.bgid$VH.get(seg);
    }

    public static void bgid$set(MemorySegment seg, short x) {
        io_uring_buf_reg.bgid$VH.set(seg, x);
    }

    public static short bgid$get(MemorySegment seg, long index) {
        return (short) io_uring_buf_reg.bgid$VH.get(seg.asSlice(index * sizeof()));
    }

    public static void bgid$set(MemorySegment seg, long index, short x) {
        io_uring_buf_reg.bgid$VH.set(seg.asSlice(index * sizeof()), x);
    }

    static final VarHandle pad$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("pad"));

    public static VarHandle pad$VH() {
        return io_uring_buf_reg.pad$VH;
    }

    public static short pad$get(MemorySegment seg) {
        return (short) io_uring_buf_reg.pad$VH.get(seg);
    }

    public static void pad$set(MemorySegment seg, short x) {
        io_uring_buf_reg.pad$VH.set(seg, x);
    }

    public static short pad$get(MemorySegment seg, long index) {
        return (short) io_uring_buf_reg.pad$VH.get(seg.asSlice(index * sizeof()));
    }

    public static void pad$set(MemorySegment seg, long index, short x) {
        io_uring_buf_reg.pad$VH.set(seg.asSlice(index * sizeof()), x);
    }

    public static MemorySegment resv$slice(MemorySegment seg) {
        return seg.asSlice(16, 24);
    }

    public static long sizeof() {
        return $LAYOUT().byteSize();
    }

    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate($LAYOUT());
    }

    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }

    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) {
        return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session);
    }
}