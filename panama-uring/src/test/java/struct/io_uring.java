package struct;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.*;

public class io_uring {
    public static final StructLayout layout = MemoryLayout.structLayout(
            MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("khead"),
                    ValueLayout.ADDRESS.withName("ktail"),
                    ValueLayout.ADDRESS.withName("kring_mask"),
                    ValueLayout.ADDRESS.withName("kring_entries"),
                    ValueLayout.ADDRESS.withName("kflags"),
                    ValueLayout.ADDRESS.withName("kdropped"),
                    ValueLayout.ADDRESS.withName("array"),
                    ValueLayout.ADDRESS.withName("sqes"),
                    JAVA_INT.withName("sqe_head"),
                    JAVA_INT.withName("sqe_tail"),
                    JAVA_LONG.withName("ring_sz"),
                    ValueLayout.ADDRESS.withName("ring_ptr"),
                    JAVA_INT.withName("ring_mask"),
                    JAVA_INT.withName("ring_entries"),
                    MemoryLayout.sequenceLayout(2, JAVA_INT).withName("pad")
            ).withName("sq"),
            MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("khead"),
                    ValueLayout.ADDRESS.withName("ktail"),
                    ValueLayout.ADDRESS.withName("kring_mask"),
                    ValueLayout.ADDRESS.withName("kring_entries"),
                    ValueLayout.ADDRESS.withName("kflags"),
                    ValueLayout.ADDRESS.withName("koverflow"),
                    ValueLayout.ADDRESS.withName("cqes"),
                    JAVA_LONG.withName("ring_sz"),
                    ValueLayout.ADDRESS.withName("ring_ptr"),
                    JAVA_INT.withName("ring_mask"),
                    JAVA_INT.withName("ring_entries"),
                    MemoryLayout.sequenceLayout(2, JAVA_INT).withName("pad")
            ).withName("cq"),
            JAVA_INT.withName("flags"),
            JAVA_INT.withName("ring_fd"),
            JAVA_INT.withName("features"),
            JAVA_INT.withName("enter_ring_fd"),
            JAVA_BYTE.withName("int_flags"),
            MemoryLayout.sequenceLayout(3, JAVA_BYTE).withName("pad"),
            JAVA_INT.withName("pad2")
    ).withName("io_uring");
}
