// Generated by jextract

package top.dreamlike.nativeLib.inet;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class ipv6_mreq {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        MemoryLayout.structLayout(
            MemoryLayout.unionLayout(
                MemoryLayout.sequenceLayout(16, Constants$root.C_CHAR$LAYOUT).withName("__u6_addr8"),
                MemoryLayout.sequenceLayout(8, Constants$root.C_SHORT$LAYOUT).withName("__u6_addr16"),
                MemoryLayout.sequenceLayout(4, Constants$root.C_INT$LAYOUT).withName("__u6_addr32")
            ).withName("__in6_u")
        ).withName("ipv6mr_multiaddr"),
        Constants$root.C_INT$LAYOUT.withName("ipv6mr_interface")
    ).withName("ipv6_mreq");
    public static MemoryLayout $LAYOUT() {
        return ipv6_mreq.$struct$LAYOUT;
    }
    public static MemorySegment ipv6mr_multiaddr$slice(MemorySegment seg) {
        return seg.asSlice(0, 16);
    }
    static final VarHandle ipv6mr_interface$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("ipv6mr_interface"));
    public static VarHandle ipv6mr_interface$VH() {
        return ipv6_mreq.ipv6mr_interface$VH;
    }
    public static int ipv6mr_interface$get(MemorySegment seg) {
        return (int)ipv6_mreq.ipv6mr_interface$VH.get(seg);
    }
    public static void ipv6mr_interface$set( MemorySegment seg, int x) {
        ipv6_mreq.ipv6mr_interface$VH.set(seg, x);
    }
    public static int ipv6mr_interface$get(MemorySegment seg, long index) {
        return (int)ipv6_mreq.ipv6mr_interface$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void ipv6mr_interface$set(MemorySegment seg, long index, int x) {
        ipv6_mreq.ipv6mr_interface$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


