package top.dreamlike

import io.vertx.core.Vertx
import io.vertx.core.impl.VertxImpl
import top.dreamlike.eventloop.IOUringEventLoop
import top.dreamlike.helper.FileOp
import top.dreamlike.helper.NativeHelper
import java.util.concurrent.ConcurrentHashMap

data class IOUringOption(val sqeSize: Int = 32, val bufferPoolSize: Int = 16, val autoSubmitDuration: Long = 1000)

private val hasStartIOUring = ConcurrentHashMap<Vertx, IOUringEventLoop>()


fun Vertx.startIOUring(option: IOUringOption): IOUringEventLoop {
    return hasStartIOUring.computeIfAbsent(this) {
        IOUringEventLoop(
            option.sqeSize,
            option.bufferPoolSize,
            option.autoSubmitDuration
        )
            .also { eventLoop ->
                eventLoop.start()
                (this as VertxImpl).addCloseHook {
                    eventLoop.shutdown()
                    it.complete()
                }
            }
    }
}


fun Vertx.openAsyncFile(path: String, vararg ops: FileOp): IOUringAsyncFile {
    val ioUringEventLoop = hasStartIOUring[this] ?: throw IllegalStateException("dont start io_uring")
    return IOUringAsyncFile(path, ioUringEventLoop, NativeHelper.parseFlag(*ops), this)
}

class DeferrableScope {
    private val defers = mutableListOf<() -> Unit>()

    companion object {
        inline fun <T> deferrableScope(fn: DeferrableScope.() -> T): T {
            val scope = DeferrableScope()
            try {
                return fn(scope)
            } finally {
                scope.end()
            }

        }
    }

    fun defer(fn: () -> Unit) {
        defers.add(fn)
    }

    fun end() {
        for (defer in defers) {
            defer()
        }
    }
}

