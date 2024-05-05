package top.dreamlike.panama.uring.nativelib.exception;

import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;

public class SyscallException extends RuntimeException {

    final int errorno;

    public SyscallException(int errorno) {
        super("syscall error: " + NativeHelper.getErrorStr(-errorno));
        this.errorno = -errorno;
    }

    public int getErrorno() {
        return errorno;
    }

    public boolean isCancel() {
        return errorno == Libc.Error_H.ECANCELED;
    }

    public boolean isNoBuffer() {
        return errorno == Libc.Error_H.ENOBUFS;
    }
}
