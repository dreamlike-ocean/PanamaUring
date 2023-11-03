package top.dreamlike.panama.genertor.exception;

public class StructException extends RuntimeException {
    public StructException(String message) {
        super(message);
    }

    public StructException(String message, Throwable cause) {
        super(message, cause);
    }
}
