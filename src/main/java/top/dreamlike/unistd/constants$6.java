// Generated by jextract

package top.dreamlike.unistd;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$6 {

    static final FunctionDescriptor sysconf$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle sysconf$MH = RuntimeHelper.downcallHandle(
        "sysconf",
        constants$6.sysconf$FUNC
    );
    static final FunctionDescriptor confstr$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle confstr$MH = RuntimeHelper.downcallHandle(
        "confstr",
        constants$6.confstr$FUNC
    );
    static final FunctionDescriptor getpid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getpid$MH = RuntimeHelper.downcallHandle(
        "getpid",
        constants$6.getpid$FUNC
    );
    static final FunctionDescriptor getppid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getppid$MH = RuntimeHelper.downcallHandle(
        "getppid",
        constants$6.getppid$FUNC
    );
    static final FunctionDescriptor getpgrp$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getpgrp$MH = RuntimeHelper.downcallHandle(
        "getpgrp",
        constants$6.getpgrp$FUNC
    );
    static final FunctionDescriptor __getpgid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle __getpgid$MH = RuntimeHelper.downcallHandle(
        "__getpgid",
        constants$6.__getpgid$FUNC
    );
}


