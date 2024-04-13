package top.dreamlike.panama.uring.nativelib.exception;

import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;

public class ErrorKernelVersionException extends RuntimeException {

    private final static String ERROR_MESSAGE_TEMPLATE =
            "Need current linux kernel > %d.%d, please upgrade your kernel. Current kernel version is %d.%d";

    private final static String ERROR_OS = "Current OS is not Linux";

    public final int allowMajor;

    public final int allowMinor;

    public ErrorKernelVersionException(int allowMajor, int allowMinor) {
        super(String.format(ERROR_MESSAGE_TEMPLATE, allowMajor, allowMinor, NativeHelper.currentLinuxMajor, NativeHelper.currentLinuxMinor));
        this.allowMajor = allowMajor;
        this.allowMinor = allowMinor;
    }

    public ErrorKernelVersionException() {
        super(ERROR_OS);
        this.allowMajor = -1;
        this.allowMinor = -1;
    }
}
