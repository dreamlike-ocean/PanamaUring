// Generated by jextract

package top.dreamlike.nativeLib.inet;

import top.dreamlike.common.CType;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$1 {

    static final FunctionDescriptor select$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle select$MH = RuntimeHelper.downcallHandle(
        "select",
        constants$1.select$FUNC
    );
    static final FunctionDescriptor pselect$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle pselect$MH = RuntimeHelper.downcallHandle(
        "pselect",
        constants$1.pselect$FUNC
    );
    static final FunctionDescriptor __cmsg_nxthdr$FUNC = FunctionDescriptor.of(CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle __cmsg_nxthdr$MH = RuntimeHelper.downcallHandle(
        "__cmsg_nxthdr",
        constants$1.__cmsg_nxthdr$FUNC
    );
    static final FunctionDescriptor __kernel_sighandler_t$FUNC = FunctionDescriptor.ofVoid(
        CType.C_INT$LAYOUT
    );
    static final MethodHandle __kernel_sighandler_t$MH = RuntimeHelper.downcallHandle(
        constants$1.__kernel_sighandler_t$FUNC
    );
    static final FunctionDescriptor socket$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT
    );
    static final MethodHandle socket$MH = RuntimeHelper.downcallHandle(
        "socket",
        constants$1.socket$FUNC
    );
}


