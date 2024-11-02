package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.trait.OwnershipResource;

import static top.dreamlike.panama.uring.nativelib.libs.Libc.Error_H.ECANCELED;

public record BufferResult<T extends OwnershipResource>(T buffer, int syscallRes) {

    public boolean canceled() {
        return syscallRes == -ECANCELED;
    }

}
