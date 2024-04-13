package struct;


import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;

public class io_uring_params {
    public static final GroupLayout layout = MemoryLayout.structLayout(
            CType.C_INT$LAYOUT.withName("sq_entries"),
            CType.C_INT$LAYOUT.withName("cq_entries"),
            CType.C_INT$LAYOUT.withName("flags"),
            CType.C_INT$LAYOUT.withName("sq_thread_cpu"),
            CType.C_INT$LAYOUT.withName("sq_thread_idle"),
            CType.C_INT$LAYOUT.withName("features"),
            CType.C_INT$LAYOUT.withName("wq_fd"),
            MemoryLayout.sequenceLayout(3, CType.C_INT$LAYOUT).withName("resv"),
            MemoryLayout.structLayout(
                    CType.C_INT$LAYOUT.withName("head"),
                    CType.C_INT$LAYOUT.withName("tail"),
                    CType.C_INT$LAYOUT.withName("ring_mask"),
                    CType.C_INT$LAYOUT.withName("ring_entries"),
                    CType.C_INT$LAYOUT.withName("flags"),
                    CType.C_INT$LAYOUT.withName("dropped"),
                    CType.C_INT$LAYOUT.withName("array"),
                    CType.C_INT$LAYOUT.withName("resv1"),
                    CType.C_LONG_LONG$LAYOUT.withName("user_addr")
            ).withName("sq_off"),
            MemoryLayout.structLayout(
                    CType.C_INT$LAYOUT.withName("head"),
                    CType.C_INT$LAYOUT.withName("tail"),
                    CType.C_INT$LAYOUT.withName("ring_mask"),
                    CType.C_INT$LAYOUT.withName("ring_entries"),
                    CType.C_INT$LAYOUT.withName("overflow"),
                    CType.C_INT$LAYOUT.withName("cqes"),
                    CType.C_INT$LAYOUT.withName("flags"),
                    CType.C_INT$LAYOUT.withName("resv1"),
                    CType.C_LONG_LONG$LAYOUT.withName("user_addr")
            ).withName("cq_off")
    ).withName("io_uring_params");
}
