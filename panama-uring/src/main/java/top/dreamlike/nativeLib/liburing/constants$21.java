// Generated by jextract

package top.dreamlike.nativeLib.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$21 {

    static final FunctionDescriptor __swab64s$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle __swab64s$MH = RuntimeHelper.downcallHandle(
        "__swab64s",
        constants$21.__swab64s$FUNC
    );
    static final FunctionDescriptor __swahw32s$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle __swahw32s$MH = RuntimeHelper.downcallHandle(
        "__swahw32s",
        constants$21.__swahw32s$FUNC
    );
    static final FunctionDescriptor __swahb32s$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle __swahb32s$MH = RuntimeHelper.downcallHandle(
        "__swahb32s",
        constants$21.__swahb32s$FUNC
    );
    static final FunctionDescriptor atomic_thread_fence$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle atomic_thread_fence$MH = RuntimeHelper.downcallHandle(
        "atomic_thread_fence",
        constants$21.atomic_thread_fence$FUNC
    );
    static final FunctionDescriptor atomic_signal_fence$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle atomic_signal_fence$MH = RuntimeHelper.downcallHandle(
        "atomic_signal_fence",
        constants$21.atomic_signal_fence$FUNC
    );
    static final FunctionDescriptor atomic_flag_test_and_set$FUNC = FunctionDescriptor.of(Constants$root.C_BOOL$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle atomic_flag_test_and_set$MH = RuntimeHelper.downcallHandle(
        "atomic_flag_test_and_set",
        constants$21.atomic_flag_test_and_set$FUNC
    );
}


