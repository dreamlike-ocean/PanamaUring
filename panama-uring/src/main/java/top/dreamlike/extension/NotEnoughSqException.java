package top.dreamlike.extension;

import top.dreamlike.helper.NativeCallException;

public class NotEnoughSqException extends NativeCallException {

    public NotEnoughSqException() {
        super("没有空闲的sqe");
    }
}
