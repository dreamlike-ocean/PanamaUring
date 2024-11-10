package top.dreamlike.panama.uring.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import top.dreamlike.panama.uring.async.BufferResult
import top.dreamlike.panama.uring.async.trait.IoUringSocketOperator
import top.dreamlike.panama.uring.nativelib.exception.SyscallException
import top.dreamlike.panama.uring.nativelib.libs.LibPoll
import top.dreamlike.panama.uring.nativelib.libs.Libc
import top.dreamlike.panama.uring.trait.OwnershipMemory
import kotlin.coroutines.coroutineContext

suspend fun IoUringSocketOperator.pollableRead(
    buffer: OwnershipMemory,
    len: Int,
    flags: Int
): BufferResult<OwnershipMemory> {
    val socketOperator = this
    val eventLoop = socketOperator.owner()
    val job = coroutineContext.job

    try {
        //这里几乎是同步读取场景 所以取消读取没啥意义 所以判断取消的时候直接丢弃
        //这里无法取消读的系统调用！
        //这里是dont wait场景的取消 不需要推进op
        val fastRead = withContext(NonCancellable) {
            asyncRecv(buffer, len, flags or Libc.Socket_H.Flag.MSG_DONTWAIT).await()
        }
        if (!job.isCancelled && fastRead.syscallRes() >= 0) {
            return fastRead
        }
    } finally {
        if (job.isCancelled) {
            buffer.drop()
        }
    }

    val pollTask = eventLoop.poll(
        socketOperator,
        LibPoll.POLLIN
    )

    try {
        val pollResult = pollTask.await()

        if (pollResult < 0) {
            throw SyscallException(pollResult)
        }

        val readResult = withContext(NonCancellable) {
            asyncRecv(buffer, len, flags).await()
        }

        if (!job.isCancelled) {
            return readResult
        }

        throw CancellationException()
    } finally {
        //这里是poll/read取消 直接丢弃吧
        if (job.isCancelled) {
            buffer.drop()
        }
    }
}
