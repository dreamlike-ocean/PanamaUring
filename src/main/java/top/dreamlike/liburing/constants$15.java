// Generated by jextract

package top.dreamlike.liburing;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$15 {

    static final FunctionDescriptor ctime$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle ctime$MH = RuntimeHelper.downcallHandle(
        "ctime",
        constants$15.ctime$FUNC
    );
    static final FunctionDescriptor asctime_r$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle asctime_r$MH = RuntimeHelper.downcallHandle(
        "asctime_r",
        constants$15.asctime_r$FUNC
    );
    static final FunctionDescriptor ctime_r$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle ctime_r$MH = RuntimeHelper.downcallHandle(
        "ctime_r",
        constants$15.ctime_r$FUNC
    );
    static final  SequenceLayout __tzname$LAYOUT = MemoryLayout.sequenceLayout(2, Constants$root.C_POINTER$LAYOUT);
    static final MemorySegment __tzname$SEGMENT = RuntimeHelper.lookupGlobalVariable("__tzname", constants$15.__tzname$LAYOUT);
    static final  OfInt __daylight$LAYOUT = Constants$root.C_INT$LAYOUT;
    static final VarHandle __daylight$VH = constants$15.__daylight$LAYOUT.varHandle();
    static final MemorySegment __daylight$SEGMENT = RuntimeHelper.lookupGlobalVariable("__daylight", constants$15.__daylight$LAYOUT);
    static final  OfLong __timezone$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle __timezone$VH = constants$15.__timezone$LAYOUT.varHandle();
    static final MemorySegment __timezone$SEGMENT = RuntimeHelper.lookupGlobalVariable("__timezone", constants$15.__timezone$LAYOUT);
}


