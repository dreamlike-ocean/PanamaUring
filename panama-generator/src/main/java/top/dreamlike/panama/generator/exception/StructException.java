package top.dreamlike.panama.generator.exception;

public class StructException extends RuntimeException {
    public StructException(String message) {
        super(message);
    }

    public StructException(String message, Throwable cause) {
        super(message, cause);
    }
}
