package top.dreamlike.panama.uring.nativelib.exception;

import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;

public class SyscallException extends RuntimeException {

    final int errorno;

    public SyscallException(int errorno) {
        super("syscall error: " + DebugHelper.getErrorStr(-errorno));
        this.errorno = -errorno;
    }

    public int getErrorno() {
        return errorno;
    }
}
