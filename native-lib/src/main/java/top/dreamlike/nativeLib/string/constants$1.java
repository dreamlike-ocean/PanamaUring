// Generated by jextract

package top.dreamlike.nativeLib.string;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$1 {

    static final FunctionDescriptor strcpy$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle strcpy$MH = RuntimeHelper.downcallHandle(
        "strcpy",
        constants$1.strcpy$FUNC
    );
    static final FunctionDescriptor strncpy$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle strncpy$MH = RuntimeHelper.downcallHandle(
        "strncpy",
        constants$1.strncpy$FUNC
    );
    static final FunctionDescriptor strcat$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle strcat$MH = RuntimeHelper.downcallHandle(
        "strcat",
        constants$1.strcat$FUNC
    );
    static final FunctionDescriptor strncat$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle strncat$MH = RuntimeHelper.downcallHandle(
        "strncat",
        constants$1.strncat$FUNC
    );
    static final FunctionDescriptor strcmp$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle strcmp$MH = RuntimeHelper.downcallHandle(
        "strcmp",
        constants$1.strcmp$FUNC
    );
    static final FunctionDescriptor strncmp$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle strncmp$MH = RuntimeHelper.downcallHandle(
        "strncmp",
        constants$1.strncmp$FUNC
    );
}


