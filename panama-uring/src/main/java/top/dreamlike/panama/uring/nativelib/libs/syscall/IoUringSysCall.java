package top.dreamlike.panama.uring.nativelib.libs.syscall;

import top.dreamlike.panama.uring.nativelib.libs.LibSysCall;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class IoUringSysCall {

    private static final MethodHandle IO_URING_SETUP_MH = Linker.nativeLinker()
            .downcallHandle(FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    /*syscall number*/ ValueLayout.JAVA_LONG,
                    /*unsigned entries*/ ValueLayout.JAVA_INT,
                    /*struct io_uring_params *p*/ ValueLayout.ADDRESS
            ));

    private static final MethodHandle IO_URING_ENTER_MH = Linker.nativeLinker()
            .downcallHandle(FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    /*syscall number*/ ValueLayout.JAVA_LONG,
                    /*unsigned fd*/ ValueLayout.JAVA_INT,
                    /*unsigned to_submit*/ ValueLayout.JAVA_INT,
                    /*unsigned min_complete*/ ValueLayout.JAVA_INT,
                    /*unsigned flags*/ ValueLayout.JAVA_INT,
                    /*unsigned sig*/ ValueLayout.ADDRESS,
                    /*size_t sz*/ ValueLayout.JAVA_LONG
            ));

    private static final MethodHandle IO_URING_REGISTER_MH = Linker.nativeLinker()
            .downcallHandle(FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    /*syscall number*/ ValueLayout.JAVA_LONG,
                    /*unsigned fd*/ ValueLayout.JAVA_INT,
                    /*unsigned op*/ ValueLayout.JAVA_INT,
                    /*unsigned arg*/ ValueLayout.ADDRESS,
                    /*unsigned nr_args*/ ValueLayout.JAVA_INT
            ));


    public static int io_uring_setup(int entries, MemorySegment ioUringParamPtr) {
        try {
            return (int) IO_URING_SETUP_MH.invokeExact(
                    LibSysCall.SYSCALL_FP,
                    LibSysCall.io_uring_setup_sys_number,
                    entries,
                    ioUringParamPtr
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int io_uring_enter(int fd, int to_submit, int min_complete, int flags, MemorySegment sig, long sig_sz){
        try {
            return (int) IO_URING_ENTER_MH.invokeExact(
                    LibSysCall.SYSCALL_FP,
                    LibSysCall.io_uring_enter_sys_number,
                    fd,
                    to_submit,
                    min_complete,
                    flags,
                    sig,
                    sig_sz
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int io_uring_register(int fd, int op, MemorySegment arg, int nr_args) {
        try {
            return (int) IO_URING_REGISTER_MH.invokeExact(
                    LibSysCall.SYSCALL_FP,
                    LibSysCall.io_uring_register_sys_number,
                    fd,
                    op,
                    arg,
                    nr_args
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
