package struct;


import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;

public class io_uring_sq {
    public static final GroupLayout layout = MemoryLayout.structLayout(
            CType.C_POINTER$LAYOUT.withName("khead"),
            CType.C_POINTER$LAYOUT.withName("ktail"),
            CType.C_POINTER$LAYOUT.withName("kring_mask"),
            CType.C_POINTER$LAYOUT.withName("kring_entries"),
            CType.C_POINTER$LAYOUT.withName("kflags"),
            CType.C_POINTER$LAYOUT.withName("kdropped"),
            CType.C_POINTER$LAYOUT.withName("array"),
            CType.C_POINTER$LAYOUT.withName("sqes"),
            CType.C_INT$LAYOUT.withName("sqe_head"),
            CType.C_INT$LAYOUT.withName("sqe_tail"),
            CType.C_LONG_LONG$LAYOUT.withName("ring_sz"),
            CType.C_POINTER$LAYOUT.withName("ring_ptr"),
            CType.C_INT$LAYOUT.withName("ring_mask"),
            CType.C_INT$LAYOUT.withName("ring_entries"),
            MemoryLayout.sequenceLayout(2, CType.C_INT$LAYOUT).withName("pad")
    ).withName("io_uring_sq");

}
