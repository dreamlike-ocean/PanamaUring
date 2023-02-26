package top.dreamlike.helper;

public class NativeCallException extends RuntimeException {
    public NativeCallException(String message) {
        super(message);
    }

    public NativeCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
