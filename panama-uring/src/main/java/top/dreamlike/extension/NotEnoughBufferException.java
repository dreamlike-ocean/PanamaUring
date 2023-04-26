package top.dreamlike.extension;

import top.dreamlike.helper.NativeCallException;

public class NotEnoughBufferException extends NativeCallException {
    public NotEnoughBufferException(int gid) {
        super(String.format("gid: %d 不存在空余的内存了", gid));
    }
}
