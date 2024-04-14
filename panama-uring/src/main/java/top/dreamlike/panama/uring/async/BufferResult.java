package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

public record BufferResult<T extends OwnershipResource>(T buffer, int syscallRes) {
}
