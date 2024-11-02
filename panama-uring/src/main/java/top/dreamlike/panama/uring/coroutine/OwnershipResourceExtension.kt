package top.dreamlike.panama.uring.coroutine

import top.dreamlike.panama.uring.trait.OwnershipResource

fun List<OwnershipResource<*>>.safeBatchDrop(exceptionHandle: ((Throwable) -> Unit)? = null) {
    forEach { t ->
        try {
            t.drop()
        }catch (e: Exception) {
            exceptionHandle?.invoke(e)
        }
    }
}