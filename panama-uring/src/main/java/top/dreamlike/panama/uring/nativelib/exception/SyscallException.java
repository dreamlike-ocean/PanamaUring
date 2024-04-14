package top.dreamlike.panama.uring.nativelib.exception;

import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;

public class SyscallException extends RuntimeException {

    final int errorno;

    public SyscallException(int errorno) {
        super("syscall error: " + NativeHelper.getErrorStr(-errorno));
        this.errorno = -errorno;
    }

    public int getErrorno() {
        return errorno;
    }
}
