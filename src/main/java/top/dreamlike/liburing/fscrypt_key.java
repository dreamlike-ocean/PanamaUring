// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class fscrypt_key {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("mode"),
        MemoryLayout.sequenceLayout(64, Constants$root.C_CHAR$LAYOUT).withName("raw"),
        Constants$root.C_INT$LAYOUT.withName("size")
    ).withName("fscrypt_key");
    public static MemoryLayout $LAYOUT() {
        return fscrypt_key.$struct$LAYOUT;
    }
    static final VarHandle mode$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("mode"));
    public static VarHandle mode$VH() {
        return fscrypt_key.mode$VH;
    }
    public static int mode$get(MemorySegment seg) {
        return (int)fscrypt_key.mode$VH.get(seg);
    }
    public static void mode$set( MemorySegment seg, int x) {
        fscrypt_key.mode$VH.set(seg, x);
    }
    public static int mode$get(MemorySegment seg, long index) {
        return (int)fscrypt_key.mode$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void mode$set(MemorySegment seg, long index, int x) {
        fscrypt_key.mode$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment raw$slice(MemorySegment seg) {
        return seg.asSlice(4, 64);
    }
    static final VarHandle size$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("size"));
    public static VarHandle size$VH() {
        return fscrypt_key.size$VH;
    }
    public static int size$get(MemorySegment seg) {
        return (int)fscrypt_key.size$VH.get(seg);
    }
    public static void size$set( MemorySegment seg, int x) {
        fscrypt_key.size$VH.set(seg, x);
    }
    public static int size$get(MemorySegment seg, long index) {
        return (int)fscrypt_key.size$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void size$set(MemorySegment seg, long index, int x) {
        fscrypt_key.size$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


