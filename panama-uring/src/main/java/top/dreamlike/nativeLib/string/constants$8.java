// Generated by jextract

package top.dreamlike.nativeLib.string;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$8 {

    static final FunctionDescriptor stpcpy$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle stpcpy$MH = RuntimeHelper.downcallHandle(
        "stpcpy",
        constants$8.stpcpy$FUNC
    );
    static final FunctionDescriptor __stpncpy$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle __stpncpy$MH = RuntimeHelper.downcallHandle(
        "__stpncpy",
        constants$8.__stpncpy$FUNC
    );
    static final FunctionDescriptor stpncpy$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle stpncpy$MH = RuntimeHelper.downcallHandle(
        "stpncpy",
        constants$8.stpncpy$FUNC
    );
    static final MemoryAddress NULL$ADDR = MemoryAddress.ofLong(0L);
}


