// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$24 {

    static final FunctionDescriptor io_uring_prep_writev$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle io_uring_prep_writev$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_writev",
        constants$24.io_uring_prep_writev$FUNC
    );
    static final FunctionDescriptor io_uring_prep_write_fixed$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_write_fixed$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_write_fixed",
        constants$24.io_uring_prep_write_fixed$FUNC
    );
    static final FunctionDescriptor io_uring_prep_recvmsg$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_recvmsg$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_recvmsg",
        constants$24.io_uring_prep_recvmsg$FUNC
    );
    static final FunctionDescriptor io_uring_prep_sendmsg$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_sendmsg$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_sendmsg",
        constants$24.io_uring_prep_sendmsg$FUNC
    );
    static final FunctionDescriptor io_uring_prep_poll_add$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle io_uring_prep_poll_add$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_poll_add",
        constants$24.io_uring_prep_poll_add$FUNC
    );
    static final FunctionDescriptor io_uring_prep_poll_remove$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle io_uring_prep_poll_remove$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_poll_remove",
        constants$24.io_uring_prep_poll_remove$FUNC
    );
}


