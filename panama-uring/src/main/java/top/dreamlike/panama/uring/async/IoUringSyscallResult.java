package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.trait.OwnershipResource;

public record IoUringSyscallResult<T>(OwnershipResource<T> resource, int result) {
}
