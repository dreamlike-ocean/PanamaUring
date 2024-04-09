package top.dreamlike.panama.uring.async;

import static top.dreamlike.panama.uring.nativelib.libs.Libc.Error_H.ECANCELED;

public record IoUringSyscallResult<T>(int res, T value) {


    public boolean canceled() {
        return res == -ECANCELED;
    }
}
