package top.dreamlike

import io.netty.buffer.Unpooled
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.impl.AsyncFileImpl
import io.vertx.core.impl.ContextInternal
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.impl.InboundBuffer
import top.dreamlike.async.uring.IOUringEventLoop

class IOUringAsyncFile(
    path: String,
    private val eventLoop: IOUringEventLoop,
    ops: Int,
    private val vertx: Vertx,
    private val readBufferSize: Int = AsyncFileImpl.DEFAULT_READ_BUFFER_SIZE
) : ReadStream<Buffer> {
    val context = vertx.orCreateContext as ContextInternal
    val file = eventLoop.openFile(path, ops)

    var exceptionHandler: Handler<Throwable>? = null

    private val queue: InboundBuffer<Buffer> = InboundBuffer(context, 0)

    private var handler: Handler<Buffer>? = null

    private var endHandler: Handler<Void>? = null

    private var readPos = 0


    private var writePos = 0
    private var maxWrites = 128 * 1024
    private var lwm = maxWrites / 2
    private val overflow = false
    private var drainHandler: Handler<Void>? = null


    init {
        queue.handler {
            if (it.length() > 0) {
                handler?.handle(it)
            } else {
                endHandler?.handle(null)
            }
        }

        queue.drainHandler {
            doRead()
        }

    }

    fun read(offset: Int, length: Int): Future<Buffer> {
        val promise = context.promise<Buffer>()
        file.read(offset, length)
            .handle { t, u ->
                if (u != null) {
                    promise.fail(u)
                    return@handle
                }

                val buffer = Buffer.buffer(Unpooled.wrappedBuffer(t, 0, t.size).asReadOnly())
                promise.complete(buffer)
            }
        return promise.future()
    }

    fun write(buffer: Buffer, fileOffset: Int = -1): Future<Int> {
        val bytes = buffer.bytes
        val promise = context.promise<Int>()

        file.write(fileOffset, bytes, 0, bytes.size)
            .handle { t, u ->
                if (u != null) {
                    promise.fail(u)
                    return@handle
                }
                promise.complete(t)
            }

        return promise.future()
    }

    fun flush(): Future<Int> {
        val res = context.promise<Int>()
        file.fsync()
            .handle { t, u ->
                if (u != null) {
                    res.fail(u)
                    return@handle
                }
                res.complete(t)
            }
        return res.future()
    }

    private fun doRead() {
        read(readPos, readBufferSize)
            .onComplete {
                if (it.failed()) {
                    exceptionHandler?.handle(it.cause())
                    return@onComplete
                }
                val result = it.result()
                if (queue.write(result) && result.length() > 0) {
                    readPos += result.length()
                    doRead()
                }
            }
    }


    override fun exceptionHandler(handler: Handler<Throwable>?): IOUringAsyncFile {
        exceptionHandler = handler
        return this
    }

    fun end(): Future<Unit> {
        val promise = context.promise<Unit>()
        context.run {
            try {
                file.close()
                promise.complete()
            } catch (e: Exception) {
                promise.fail(e)
            }

        }
        return promise.future()
    }


    override fun pause(): ReadStream<Buffer> {
        queue.pause()
        return this
    }

    override fun resume(): ReadStream<Buffer> {
        queue.resume()
        return this
    }

    override fun fetch(amount: Long): ReadStream<Buffer> {
        queue.fetch(amount)
        return this
    }

    override fun endHandler(endHandler: Handler<Void>?): ReadStream<Buffer> {
        this.endHandler = endHandler
        return this
    }

    override fun handler(handler: Handler<Buffer>?): ReadStream<Buffer> {
        this.handler = handler
        if (handler == null) {
            queue.clear()
        } else {
            doRead()
        }

        return this
    }
}
