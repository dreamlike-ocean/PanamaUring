// Generated by jextract

package top.dreamlike.nativeLib.flock;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$1 {

    static final FunctionDescriptor posix_fallocate$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_LONG_LONG$LAYOUT,
            Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle posix_fallocate$MH = RuntimeHelper.downcallHandle(
            "posix_fallocate",
            constants$1.posix_fallocate$FUNC
    );
    static final FunctionDescriptor flock$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle flock$MH = RuntimeHelper.downcallHandle(
            "flock",
            constants$1.flock$FUNC
    );
}

