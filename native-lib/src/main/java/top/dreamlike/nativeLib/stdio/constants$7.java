// Generated by jextract

package top.dreamlike.nativeLib.stdio;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

class constants$7 {

    static final FunctionDescriptor getchar_unlocked$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getchar_unlocked$MH = RuntimeHelper.downcallHandle(
            "getchar_unlocked",
            constants$7.getchar_unlocked$FUNC
    );
    static final FunctionDescriptor fgetc_unlocked$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fgetc_unlocked$MH = RuntimeHelper.downcallHandle(
            "fgetc_unlocked",
            constants$7.fgetc_unlocked$FUNC
    );
    static final FunctionDescriptor fputc$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fputc$MH = RuntimeHelper.downcallHandle(
            "fputc",
            constants$7.fputc$FUNC
    );
    static final FunctionDescriptor putc$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle putc$MH = RuntimeHelper.downcallHandle(
            "putc",
            constants$7.putc$FUNC
    );
    static final FunctionDescriptor putchar$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle putchar$MH = RuntimeHelper.downcallHandle(
            "putchar",
            constants$7.putchar$FUNC
    );
    static final FunctionDescriptor fputc_unlocked$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
            Constants$root.C_INT$LAYOUT,
            Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fputc_unlocked$MH = RuntimeHelper.downcallHandle(
            "fputc_unlocked",
            constants$7.fputc_unlocked$FUNC
    );
}

