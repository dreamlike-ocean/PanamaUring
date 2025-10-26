package top.dreamlike.panama.generator.proxy;

public class ErrorNo {
    @Deprecated
    public static ThreadLocal<Integer> error = new ThreadLocal<>();

    static final ThreadLocal<CapturedErrorState> capturedError = new ThreadLocal<>();

    public sealed interface CapturedErrorState permits PosixCapturedError, WindowsCapturedError {
        int errno();
    }

    public record PosixCapturedError(int errno) implements CapturedErrorState { }

    public record WindowsCapturedError(int errno, int lastError, int wsaLastError) implements CapturedErrorState { }

    public static CapturedErrorState getCapturedError() {
        return capturedError.get();
    }
}
