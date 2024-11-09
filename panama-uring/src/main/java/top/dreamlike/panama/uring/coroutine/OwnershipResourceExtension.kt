package top.dreamlike.panama.uring.coroutine

import top.dreamlike.panama.uring.trait.OwnershipMemory
import top.dreamlike.panama.uring.trait.OwnershipResource
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

fun List<OwnershipResource<*>>.safeBatchDrop(exceptionHandle: ((Throwable) -> Unit)? = null) {
    forEach { t ->
        try {
            t.drop()
        } catch (e: Exception) {
            exceptionHandle?.invoke(e)
        }
    }
}

fun MalloceUnboundMemory(size: Long, dropCallback: (() -> Unit)? = null) = object : OwnershipMemory {

    private val _resource = Arena.global().allocate(size)

    override fun resource(): MemorySegment? {
        return _resource
    }

    override fun drop() {
        dropCallback?.invoke()
    }

}