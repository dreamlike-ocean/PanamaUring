// Generated by jextract

package top.dreamlike.nativeLib.epoll;

import top.dreamlike.common.CType;
import top.dreamlike.helper.RuntimeHelper;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$2 {

    static final FunctionDescriptor epoll_pwait$FUNC = FunctionDescriptor.of(CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_INT$LAYOUT,
        CType.C_POINTER$LAYOUT
    );
    static final MethodHandle epoll_pwait$MH = RuntimeHelper.downcallHandle(
        "epoll_pwait",
        constants$2.epoll_pwait$FUNC
    );

}


