// Generated by jextract

package top.dreamlike.nativeLib.inet;

import top.dreamlike.common.CType;

import java.lang.foreign.*;

public class fsid_t {

    static final GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(2, CType.C_INT$LAYOUT).withName("__val")
    );

    public static MemoryLayout $LAYOUT() {
        return fsid_t.$struct$LAYOUT;
    }

    public static MemorySegment __val$slice(MemorySegment seg) {
        return seg.asSlice(0, 8);
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

    public static MemorySegment ofAddress(MemorySegment addr, Arena session) {
        return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session);
    }
}


