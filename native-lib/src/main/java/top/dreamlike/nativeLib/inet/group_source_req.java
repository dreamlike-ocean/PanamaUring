// Generated by jextract

package top.dreamlike.nativeLib.inet;

import top.dreamlike.common.CType;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;

public class group_source_req {

    static final GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
            CType.C_INT$LAYOUT.withName("gsr_interface"),
            MemoryLayout.paddingLayout(32),
            MemoryLayout.structLayout(
                    CType.C_SHORT$LAYOUT.withName("ss_family"),
                    MemoryLayout.sequenceLayout(118, CType.C_CHAR$LAYOUT).withName("__ss_padding"),
                    CType.C_LONG_LONG$LAYOUT.withName("__ss_align")
            ).withName("gsr_group"),
            MemoryLayout.structLayout(
                    CType.C_SHORT$LAYOUT.withName("ss_family"),
                    MemoryLayout.sequenceLayout(118, CType.C_CHAR$LAYOUT).withName("__ss_padding"),
                    CType.C_LONG_LONG$LAYOUT.withName("__ss_align")
            ).withName("gsr_source")
    ).withName("group_source_req");

    public static MemoryLayout $LAYOUT() {
        return group_source_req.$struct$LAYOUT;
    }
    static final VarHandle gsr_interface$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("gsr_interface"));
    public static VarHandle gsr_interface$VH() {
        return group_source_req.gsr_interface$VH;
    }
    public static int gsr_interface$get(MemorySegment seg) {
        return (int)group_source_req.gsr_interface$VH.get(seg);
    }
    public static void gsr_interface$set( MemorySegment seg, int x) {
        group_source_req.gsr_interface$VH.set(seg, x);
    }
    public static int gsr_interface$get(MemorySegment seg, long index) {
        return (int)group_source_req.gsr_interface$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void gsr_interface$set(MemorySegment seg, long index, int x) {
        group_source_req.gsr_interface$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment gsr_group$slice(MemorySegment seg) {
        return seg.asSlice(8, 128);
    }
    public static MemorySegment gsr_source$slice(MemorySegment seg) {
        return seg.asSlice(136, 128);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }

    public static MemorySegment ofAddress(MemorySegment addr, Arena session) {
        return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session);
    }
}


