// Generated by jextract

package top.dreamlike.nativeLib.fcntl;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$1 {

    static final FunctionDescriptor posix_fallocate$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_LONG_LONG$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle posix_fallocate$MH = RuntimeHelper.downcallHandle(
        "posix_fallocate",
        constants$1.posix_fallocate$FUNC
    );
}


