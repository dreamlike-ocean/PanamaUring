// Generated by jextract

package top.dreamlike.nativeLib.string;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$5 {

    static final FunctionDescriptor strerror_r$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle strerror_r$MH = RuntimeHelper.downcallHandle(
        "strerror_r",
        constants$5.strerror_r$FUNC
    );
    static final FunctionDescriptor strerror_l$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle strerror_l$MH = RuntimeHelper.downcallHandle(
        "strerror_l",
        constants$5.strerror_l$FUNC
    );
    static final FunctionDescriptor bcmp$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle bcmp$MH = RuntimeHelper.downcallHandle(
        "bcmp",
        constants$5.bcmp$FUNC
    );
    static final FunctionDescriptor bcopy$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle bcopy$MH = RuntimeHelper.downcallHandle(
        "bcopy",
        constants$5.bcopy$FUNC
    );
    static final FunctionDescriptor bzero$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle bzero$MH = RuntimeHelper.downcallHandle(
        "bzero",
        constants$5.bzero$FUNC
    );
    static final FunctionDescriptor index$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle index$MH = RuntimeHelper.downcallHandle(
        "index",
        constants$5.index$FUNC
    );
}


