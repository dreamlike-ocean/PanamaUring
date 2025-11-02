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

    public enum ErrorNoType {
        AUTO(null),
        POSIX_ERROR_NO ("errno"),
        WINDOWS_GET_LAST_ERROR("GetLastError"),
        WINDOWS_WSA_GET_LAST_ERROR("WSAGetLastError");
        final String fieldName;

        ErrorNoType(String fieldName) {
            this.fieldName = fieldName;
        }
    }
}
