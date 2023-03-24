package iouring

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.await
import top.dreamlike.DeferrableScope
import top.dreamlike.IOUringOption
import top.dreamlike.helper.FileOp
import top.dreamlike.openAsyncFile
import top.dreamlike.startIOUring

suspend fun main() {
    DeferrableScope.deferrableScope {
        val vertx = Vertx.vertx()
        defer { vertx.close() }

        vertx.startIOUring(IOUringOption(32, 4))
        val asyncFile = vertx.openAsyncFile("demo.txt", FileOp.RW, FileOp.APPEND)

        val content = asyncFile.read(0, 1024).await()
        println(content)
        val res = asyncFile.write(Buffer.buffer("追加写")).await()
        println("append file res:$res")
    }
}
