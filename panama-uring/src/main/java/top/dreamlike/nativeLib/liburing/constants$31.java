// Generated by jextract

package top.dreamlike.nativeLib.liburing;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$31 {

    static final FunctionDescriptor io_uring_prep_timeout_update$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_timeout_update$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_timeout_update",
        constants$31.io_uring_prep_timeout_update$FUNC
    );
    static final FunctionDescriptor io_uring_prep_accept$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_accept$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_accept",
        constants$31.io_uring_prep_accept$FUNC
    );
    static final FunctionDescriptor io_uring_prep_cancel$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_cancel$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_cancel",
        constants$31.io_uring_prep_cancel$FUNC
    );
    static final FunctionDescriptor io_uring_prep_link_timeout$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_link_timeout$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_link_timeout",
        constants$31.io_uring_prep_link_timeout$FUNC
    );
    static final FunctionDescriptor io_uring_prep_connect$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_connect$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_connect",
        constants$31.io_uring_prep_connect$FUNC
    );
    static final FunctionDescriptor io_uring_prep_files_update$FUNC = FunctionDescriptor.ofVoid(
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle io_uring_prep_files_update$MH = RuntimeHelper.downcallHandle(
        "io_uring_prep_files_update",
        constants$31.io_uring_prep_files_update$FUNC
    );
}


