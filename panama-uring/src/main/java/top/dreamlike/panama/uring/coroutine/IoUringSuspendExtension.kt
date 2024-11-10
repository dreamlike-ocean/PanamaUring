package top.dreamlike.panama.uring.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import top.dreamlike.panama.uring.async.cancel.CancelableFuture
import top.dreamlike.panama.uring.trait.OwnershipResource
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resumeWithException


suspend inline fun <T : OwnershipResource<*>, R> ioUringOpSuspend(
    resource: T,
    lazyCancel: Boolean = true,
    crossinline action: () -> CancelableFuture<R>
) = ioUringOpSuspend(listOf(resource), lazyCancel, action)

suspend inline fun <T : OwnershipResource<*>, R> ioUringOpSuspend(
    resources: List<T>,
    lazyCancel: Boolean = true,
    crossinline action: () -> CancelableFuture<R>
) = suspendCancellableCoroutine { continuation ->
    val task = action()
    continuation.invokeOnCancellation { cause ->
        if (cause is CancellationException) {
            val ignore = task.ioUringCancel(!lazyCancel)
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


suspend fun <T> CancelableFuture<T>.await(needPushCancelOp : Boolean = true ,needSubmit: Boolean = false): T {
    if (this.isDone) {
        try {
            @Suppress("UNCHECKED_CAST", "BlockingMethodInNonBlockingContext")
            return this.get() as T
        } catch (e: ExecutionException) {
            throw e.cause ?: e // unwrap original cause from ExecutionException
        }
    }

    return suspendCancellableCoroutine { continuation ->
        this.handle { t, u ->
            continuation.invokeOnCancellation { cause ->
                if (needPushCancelOp) {
                    this.ioUringCancel(needSubmit)
                }
            }

            if (continuation.isCancelled) {
                return@handle
            }

            if (u == null) {
                continuation.resume(t!!) { _, _, _ -> }
            } else {
                continuation.resumeWithException(u)
            }
        }
    }
}