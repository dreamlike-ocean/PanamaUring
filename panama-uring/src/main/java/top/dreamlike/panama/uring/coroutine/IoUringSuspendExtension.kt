package top.dreamlike.panama.uring.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import top.dreamlike.panama.uring.async.cancel.CancelableFuture
import top.dreamlike.panama.uring.trait.OwnershipResource
import kotlin.coroutines.resumeWithException


suspend inline fun <T : OwnershipResource<*>, R> ioUringOpSuspend(
    resource: T,
    lazyCancel: Boolean = false,
    crossinline action: () -> CancelableFuture<R>
) = ioUringOpSuspend(listOf(resource), lazyCancel, action)

suspend inline fun <T : OwnershipResource<*>, R> ioUringOpSuspend(
    resources: List<T>,
    lazyCancel: Boolean = false,
    crossinline action: () -> CancelableFuture<R>
) = suspendCancellableCoroutine { continuation ->
    val task = action()
    continuation.invokeOnCancellation { cause ->
        if (cause is CancellationException) {
            val ignore = task.ioUringCancel(lazyCancel)
        }
    }
    task.handle { t, u ->
        if (task.isCancelled) {
            resources.safeBatchDrop()
            return@handle
        }
        if (u == null) {
            continuation.resume(t!!) { _, _, _ -> }
        } else {
            continuation.resumeWithException(u)
        }
    }
}
