// Generated by jextract

package top.dreamlike.nativeLib.eventfd;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
public class eventfd_h  {

    /* package-private */ eventfd_h() {}
    public static int _SYS_EVENTFD_H() {
        return (int)1L;
    }


    public static int EFD_SEMAPHORE() {
        return (int)1L;
    }
    public static int EFD_CLOEXEC() {
        return (int)524288L;
    }
    public static int EFD_NONBLOCK() {
        return (int)2048L;
    }
    private static MethodHandle eventfd$MH() {
        return RuntimeHelper.requireNonNull(constants$0.eventfd$MH,"eventfd");
    }
    public static int eventfd ( int __count,  int __flags) {
        var mh$ = eventfd$MH();
        try {
            return (int)mh$.invokeExact(__count, __flags);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    private static MethodHandle eventfd_read$MH() {
        return RuntimeHelper.requireNonNull(constants$0.eventfd_read$MH, "eventfd_read");
    }

    public static int eventfd_read(int __fd, MemorySegment __value) {
        var mh$ = eventfd_read$MH();
        try {
            return (int) mh$.invokeExact(__fd, __value);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    private static MethodHandle eventfd_write$MH() {
        return RuntimeHelper.requireNonNull(constants$0.eventfd_write$MH, "eventfd_write");
    }

    public static int eventfd_write ( int __fd,  long __value) {
        var mh$ = eventfd_write$MH();
        try {
            return (int)mh$.invokeExact(__fd, __value);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
}


