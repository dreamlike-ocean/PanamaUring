// Generated by jextract

package top.dreamlike.nativeLib.in;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$4 {

    static final FunctionDescriptor setsockopt$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle setsockopt$MH = RuntimeHelper.downcallHandle(
        "setsockopt",
        constants$4.setsockopt$FUNC
    );
    static final FunctionDescriptor listen$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle listen$MH = RuntimeHelper.downcallHandle(
        "listen",
        constants$4.listen$FUNC
    );
    static final FunctionDescriptor accept$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle accept$MH = RuntimeHelper.downcallHandle(
        "accept",
        constants$4.accept$FUNC
    );
    static final FunctionDescriptor shutdown$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle shutdown$MH = RuntimeHelper.downcallHandle(
        "shutdown",
        constants$4.shutdown$FUNC
    );
    static final FunctionDescriptor sockatmark$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle sockatmark$MH = RuntimeHelper.downcallHandle(
        "sockatmark",
        constants$4.sockatmark$FUNC
    );
    static final FunctionDescriptor isfdtype$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle isfdtype$MH = RuntimeHelper.downcallHandle(
        "isfdtype",
        constants$4.isfdtype$FUNC
    );
}


