package top.dreamlike.panama.uring.coroutine

import top.dreamlike.panama.generator.proxy.NativeArrayPointer
import top.dreamlike.panama.uring.async.fd.IoUringAsyncFd
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec
import top.dreamlike.panama.uring.trait.OwnershipMemory
import top.dreamlike.panama.uring.trait.OwnershipResource


suspend fun IoUringAsyncFd.readSuspend(
    buffer: OwnershipMemory,
    offset: Int,
    length: Int,
    lazyCancel: Boolean = false
) = ioUringOpSuspend(buffer, lazyCancel) {
    asyncRead(buffer, offset, length)
}

suspend fun IoUringAsyncFd.readVSuspend(
    iovec: OwnershipResource<NativeArrayPointer<Iovec>>,
    nr_vecs: Int,
    offset: Int,
    lazyCancel: Boolean = false
) = ioUringOpSuspend(iovec, lazyCancel) {
    asyncReadV(iovec, nr_vecs, offset)
}


suspend fun IoUringAsyncFd.writeSuspend(
    buffer: OwnershipMemory,
    offset: Int,
    length: Int,
    lazyCancel: Boolean = false
) = ioUringOpSuspend(buffer, lazyCancel) {
    asyncWrite(buffer, offset, length)
}

suspend fun IoUringAsyncFd.writeVSuspend(
    iovec: OwnershipResource<NativeArrayPointer<Iovec>>,
    nr_vecs: Int,
    offset: Int,
    lazyCancel: Boolean = false
) = ioUringOpSuspend(iovec, lazyCancel) {
    asyncWriteV(iovec, nr_vecs, offset)
}