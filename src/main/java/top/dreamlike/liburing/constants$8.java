// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$8 {

    static final FunctionDescriptor sig_t$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle sig_t$MH = RuntimeHelper.downcallHandle(
        constants$8.sig_t$FUNC
    );
    static final FunctionDescriptor sigemptyset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle sigemptyset$MH = RuntimeHelper.downcallHandle(
        "sigemptyset",
        constants$8.sigemptyset$FUNC
    );
    static final FunctionDescriptor sigfillset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle sigfillset$MH = RuntimeHelper.downcallHandle(
        "sigfillset",
        constants$8.sigfillset$FUNC
    );
    static final FunctionDescriptor sigaddset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle sigaddset$MH = RuntimeHelper.downcallHandle(
        "sigaddset",
        constants$8.sigaddset$FUNC
    );
    static final FunctionDescriptor sigdelset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle sigdelset$MH = RuntimeHelper.downcallHandle(
        "sigdelset",
        constants$8.sigdelset$FUNC
    );
}


