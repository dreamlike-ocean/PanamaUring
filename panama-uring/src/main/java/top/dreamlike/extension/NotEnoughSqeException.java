package top.dreamlike.extension;

import top.dreamlike.helper.NativeCallException;

public class NotEnoughSqeException extends NativeCallException {

    public NotEnoughSqeException() {
        super("io_uring dont have enough sqe");
    }
}
