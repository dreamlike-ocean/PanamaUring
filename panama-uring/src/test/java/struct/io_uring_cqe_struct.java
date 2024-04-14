package struct;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.*;

public class io_uring_cqe_struct {
    public static final StructLayout layout = MemoryLayout.structLayout(
            JAVA_LONG.withName("user_data"),
            JAVA_INT.withName("res"),
            JAVA_INT.withName("flags"),
            MemoryLayout.sequenceLayout(0, JAVA_LONG).withName("big_cqe")
    ).withName("io_uring_cqe");
}
