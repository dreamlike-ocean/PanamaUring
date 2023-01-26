package top.dreamlike

import io.vertx.core.Vertx
import io.vertx.core.impl.VertxImpl
import top.dreamlike.async.uring.IOUringEventLoop
import java.util.concurrent.ConcurrentHashMap

data class IOUringOption(val sqeSize: Int = 32, val bufferPoolSize: Int = 16, val autoSubmitDuration: Long = 1000)

private val hasStartIOUring = ConcurrentHashMap<Vertx, IOUringEventLoop>()


fun Vertx.startIOUring(option: IOUringOption): IOUringEventLoop {
    return hasStartIOUring.computeIfAbsent(this) {
        IOUringEventLoop(option.sqeSize, option.bufferPoolSize, option.autoSubmitDuration)
            .also { eventLoop ->
                eventLoop.start()
                (this as VertxImpl).addCloseHook {
                    eventLoop.shutdown()
                    it.complete()
                }
            }
    }
}


fun Vertx.openAsyncFile(path: String, ops: Int): IOUringAsyncFile {
    val ioUringEventLoop = hasStartIOUring[this] ?: throw IllegalStateException("dont start io_uring")
    return IOUringAsyncFile(path, ioUringEventLoop, ops, this)
}

