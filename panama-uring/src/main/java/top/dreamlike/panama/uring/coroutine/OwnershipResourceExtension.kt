package top.dreamlike.panama.uring.coroutine

import top.dreamlike.panama.uring.trait.OwnershipMemory
import top.dreamlike.panama.uring.trait.OwnershipResource
import java.lang.foreign.Arena

fun List<OwnershipResource<*>>.safeBatchDrop(exceptionHandle: ((Throwable) -> Unit)? = null) {
    forEach { t ->
        try {
            t.drop()
        } catch (e: Exception) {
            exceptionHandle?.invoke(e)
        }
    }
}


fun MalloceUnboundMemory(size: Long) = OwnershipMemory.of(Arena.global().allocate(size))