// Generated by jextract

package top.dreamlike.nativeLib.stdio;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$3 {

    static final FunctionDescriptor setbuf$FUNC = FunctionDescriptor.ofVoid(
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle setbuf$MH = RuntimeHelper.downcallHandle(
            "setbuf",
            constants$3.setbuf$FUNC
    );
    static final FunctionDescriptor setvbuf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle setvbuf$MH = RuntimeHelper.downcallHandle(
            "setvbuf",
            constants$3.setvbuf$FUNC
    );
    static final FunctionDescriptor setbuffer$FUNC = FunctionDescriptor.ofVoid(
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle setbuffer$MH = RuntimeHelper.downcallHandle(
            "setbuffer",
            constants$3.setbuffer$FUNC
    );
    static final FunctionDescriptor setlinebuf$FUNC = FunctionDescriptor.ofVoid(
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle setlinebuf$MH = RuntimeHelper.downcallHandle(
            "setlinebuf",
            constants$3.setlinebuf$FUNC
    );
    static final FunctionDescriptor fprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fprintf$MH = RuntimeHelper.downcallHandleVariadic(
            "fprintf",
            constants$3.fprintf$FUNC
    );
    static final FunctionDescriptor printf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle printf$MH = RuntimeHelper.downcallHandleVariadic(
            "printf",
            constants$3.printf$FUNC
    );
}


