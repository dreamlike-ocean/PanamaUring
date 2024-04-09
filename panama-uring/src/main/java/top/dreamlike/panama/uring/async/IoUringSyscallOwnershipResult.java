package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.trait.OwnershipResource;

import static top.dreamlike.panama.uring.nativelib.libs.Libc.Error_H.ECANCELED;

public record IoUringSyscallOwnershipResult<T>(OwnershipResource<T> resource, int result) {

    public boolean canceled() {
        return result == -ECANCELED;
    }
}
