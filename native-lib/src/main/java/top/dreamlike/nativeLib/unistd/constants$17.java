// Generated by jextract

package top.dreamlike.nativeLib.unistd;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$17 {

    static final FunctionDescriptor getdtablesize$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT);
    static final MethodHandle getdtablesize$MH = RuntimeHelper.downcallHandle(
        "getdtablesize",
        constants$17.getdtablesize$FUNC
    );
    static final FunctionDescriptor truncate$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle truncate$MH = RuntimeHelper.downcallHandle(
        "truncate",
        constants$17.truncate$FUNC
    );
    static final FunctionDescriptor ftruncate$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle ftruncate$MH = RuntimeHelper.downcallHandle(
        "ftruncate",
        constants$17.ftruncate$FUNC
    );
    static final FunctionDescriptor brk$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle brk$MH = RuntimeHelper.downcallHandle(
        "brk",
        constants$17.brk$FUNC
    );
    static final FunctionDescriptor sbrk$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle sbrk$MH = RuntimeHelper.downcallHandle(
        "sbrk",
        constants$17.sbrk$FUNC
    );
    static final FunctionDescriptor syscall$FUNC = FunctionDescriptor.of(CType.C_LONG_LONG$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle syscall$MH = RuntimeHelper.downcallHandleVariadic(
        "syscall",
        constants$17.syscall$FUNC
    );
}


