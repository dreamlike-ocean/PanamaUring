// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$23 {

    static final FunctionDescriptor io_uring_sqe_set_data$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle io_uring_sqe_set_data$MH = RuntimeHelper.downcallHandle(
        "io_uring_sqe_set_data",
        constants$23.io_uring_sqe_set_data$FUNC
    );
    static final FunctionDescriptor io_uring_cqe_get_data$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle io_uring_cqe_get_data$MH = RuntimeHelper.downcallHandle(
        "io_uring_cqe_get_data",
        constants$23.io_uring_cqe_get_data$FUNC
    );
    static final FunctionDescriptor io_uring_sqe_set_flags$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_sqe_set_flags$MH = RuntimeHelper.downcallHandle(
        "io_uring_sqe_set_flags",
        constants$23.io_uring_sqe_set_flags$FUNC
    );
    static final FunctionDescriptor io_uring_prep_rw$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle io_uring_prep_rw$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_rw",
        constants$23.io_uring_prep_rw$FUNC
    );
    static final FunctionDescriptor io_uring_prep_readv$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle io_uring_prep_readv$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_readv",
        constants$23.io_uring_prep_readv$FUNC
    );
    static final FunctionDescriptor io_uring_prep_read_fixed$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_read_fixed$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_read_fixed",
        constants$23.io_uring_prep_read_fixed$FUNC
    );
}


