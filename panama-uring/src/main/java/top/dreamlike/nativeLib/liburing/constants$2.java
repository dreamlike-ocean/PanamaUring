// Generated by jextract

package top.dreamlike.nativeLib.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$2 {

    static final FunctionDescriptor recvmsg$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle recvmsg$MH = RuntimeHelper.downcallHandle(
        "recvmsg",
        constants$2.recvmsg$FUNC
    );
    static final FunctionDescriptor getsockopt$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle getsockopt$MH = RuntimeHelper.downcallHandle(
        "getsockopt",
        constants$2.getsockopt$FUNC
    );
    static final FunctionDescriptor setsockopt$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setsockopt$MH = RuntimeHelper.downcallHandle(
        "setsockopt",
        constants$2.setsockopt$FUNC
    );
    static final FunctionDescriptor listen$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle listen$MH = RuntimeHelper.downcallHandle(
        "listen",
        constants$2.listen$FUNC
    );
    static final FunctionDescriptor accept$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle accept$MH = RuntimeHelper.downcallHandle(
        "accept",
        constants$2.accept$FUNC
    );
    static final FunctionDescriptor shutdown$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle shutdown$MH = RuntimeHelper.downcallHandle(
        "shutdown",
        constants$2.shutdown$FUNC
    );
}


