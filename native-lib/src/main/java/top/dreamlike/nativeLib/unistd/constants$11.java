// Generated by jextract

package top.dreamlike.nativeLib.unistd;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$11 {

    static final FunctionDescriptor symlink$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle symlink$MH = RuntimeHelper.downcallHandle(
        "symlink",
        constants$11.symlink$FUNC
    );
    static final FunctionDescriptor readlink$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle readlink$MH = RuntimeHelper.downcallHandle(
        "readlink",
        constants$11.readlink$FUNC
    );
    static final FunctionDescriptor symlinkat$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle symlinkat$MH = RuntimeHelper.downcallHandle(
        "symlinkat",
        constants$11.symlinkat$FUNC
    );
    static final FunctionDescriptor readlinkat$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle readlinkat$MH = RuntimeHelper.downcallHandle(
        "readlinkat",
        constants$11.readlinkat$FUNC
    );
    static final FunctionDescriptor unlink$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle unlink$MH = RuntimeHelper.downcallHandle(
        "unlink",
        constants$11.unlink$FUNC
    );
    static final FunctionDescriptor unlinkat$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle unlinkat$MH = RuntimeHelper.downcallHandle(
        "unlinkat",
        constants$11.unlinkat$FUNC
    );
}

