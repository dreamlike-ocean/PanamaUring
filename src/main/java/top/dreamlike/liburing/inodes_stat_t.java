// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class inodes_stat_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("nr_inodes"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("nr_unused"),
        MemoryLayout.sequenceLayout(5, Constants$root.C_LONG_LONG$LAYOUT).withName("dummy")
    ).withName("inodes_stat_t");
    public static MemoryLayout $LAYOUT() {
        return inodes_stat_t.$struct$LAYOUT;
    }
    static final VarHandle nr_inodes$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("nr_inodes"));
    public static VarHandle nr_inodes$VH() {
        return inodes_stat_t.nr_inodes$VH;
    }
    public static long nr_inodes$get(MemorySegment seg) {
        return (long)inodes_stat_t.nr_inodes$VH.get(seg);
    }
    public static void nr_inodes$set( MemorySegment seg, long x) {
        inodes_stat_t.nr_inodes$VH.set(seg, x);
    }
    public static long nr_inodes$get(MemorySegment seg, long index) {
        return (long)inodes_stat_t.nr_inodes$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void nr_inodes$set(MemorySegment seg, long index, long x) {
        inodes_stat_t.nr_inodes$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle nr_unused$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("nr_unused"));
    public static VarHandle nr_unused$VH() {
        return inodes_stat_t.nr_unused$VH;
    }
    public static long nr_unused$get(MemorySegment seg) {
        return (long)inodes_stat_t.nr_unused$VH.get(seg);
    }
    public static void nr_unused$set( MemorySegment seg, long x) {
        inodes_stat_t.nr_unused$VH.set(seg, x);
    }
    public static long nr_unused$get(MemorySegment seg, long index) {
        return (long)inodes_stat_t.nr_unused$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void nr_unused$set(MemorySegment seg, long index, long x) {
        inodes_stat_t.nr_unused$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment dummy$slice(MemorySegment seg) {
        return seg.asSlice(16, 40);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


