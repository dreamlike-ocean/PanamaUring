import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory
import top.dreamlike.panama.generator.proxy.NativeArrayPointer
import top.dreamlike.panama.generator.proxy.StructProxyGenerator
import top.dreamlike.panama.uring.async.fd.AsyncEventFd
import top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd
import top.dreamlike.panama.uring.coroutine.*
import top.dreamlike.panama.uring.nativelib.Instance
import top.dreamlike.panama.uring.nativelib.exception.SyscallException
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec
import top.dreamlike.panama.uring.trait.OwnershipResource
import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class CoroutineTest {

    companion object {
        val log = LoggerFactory.getLogger(CoroutineTest::class.java)
    }

    @Test
    fun testCoroutineFd(): Unit = runBlocking {
        IoUringEventLoopGetter.get(IoUringEventLoopGetter.EventLoopType.Original) {
            it.sq_entries = 4
            it.flags = 0
        }.use { eventLoop ->
            eventLoop.start()
            val eventFd = AsyncEventFd(eventLoop)
            log.info("eventfd : ${eventFd.fd()}")

            //write
            val writeBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize())
            writeBuffer.resource().set(ValueLayout.JAVA_LONG, 0, 1024)
            val writeResult = eventFd.writeSuspend(writeBuffer, 0, ValueLayout.JAVA_LONG.byteSize().toInt())
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize().toInt(), writeResult.syscallRes)


            //read
            val readBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize())
            val readResult = eventFd.readSuspend(readBuffer, 0, ValueLayout.JAVA_LONG.byteSize().toInt())
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize().toInt(), readResult.syscallRes)
            Assert.assertEquals(1024, readBuffer.resource().get(ValueLayout.JAVA_LONG, 0))

            //writeV
            val iovec = Instance.STRUCT_PROXY_GENERATOR.allocate(Arena.global(), Iovec::class.java)
            val writeVBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize())
            writeVBuffer.resource().set(ValueLayout.JAVA_LONG, 0, 1024 * 2)
            iovec.iov_base = writeVBuffer.resource()
            iovec.iov_len = ValueLayout.JAVA_LONG.byteSize()
            val pointer = NativeArrayPointer(
                Instance.STRUCT_PROXY_GENERATOR,
                StructProxyGenerator.findMemorySegment(iovec),
                Iovec::class.java
            )
            val asyncWriteVResult = eventFd.writeVSuspend(OwnershipResource.wrap<_>(pointer), 1, 0)
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize().toInt(), asyncWriteVResult.syscallRes)

            //readV
            val readVBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize())
            iovec.iov_base = readVBuffer.resource()
            iovec.iov_len = ValueLayout.JAVA_LONG.byteSize()
            val asyncReadVResult = eventFd.readVSuspend(OwnershipResource.wrap<_>(pointer), 1, 0)
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize().toInt(), asyncReadVResult.syscallRes)
            Assert.assertEquals(1024 * 2, readVBuffer.resource().get(ValueLayout.JAVA_LONG, 0))

        }
    }


    @Test
    fun testCancel(): Unit = runBlocking {
        IoUringEventLoopGetter.get(IoUringEventLoopGetter.EventLoopType.Original) {
            it.sq_entries = 4
            it.flags = 0
        }.use { eventLoop ->
            eventLoop.start()
            val eventFd = AsyncEventFd(eventLoop)
            val latch = CountDownLatch(1)
            val readBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize()) {
                latch.countDown()
            }
            val job = async(coroutineContext) {
                eventFd.readSuspend(readBuffer, 0, ValueLayout.JAVA_LONG.byteSize().toInt())
            }
            delay(1000)
            job.cancel()
            latch.await()
        }
    }

    @Test
    fun testPollSocket() = runBlocking {
        IoUringEventLoopGetter.get(IoUringEventLoopGetter.EventLoopType.Original) {
            it.sq_entries = 4
            it.flags = 0
        }.use { eventLoop ->
            eventLoop.start()
            val socketPath = File("/tmp/pollSocket.sock")
            socketPath.deleteOnExit()
            val socketRef = AtomicReference<SocketChannel>()
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { serverSocket ->
                Thread.startVirtualThread {
                    serverSocket.bind(UnixDomainSocketAddress.of(socketPath.toPath()))
                    val accept = serverSocket.accept()
                    log.info("accept : {}", socketPath)
                    socketRef.set(accept)
                    //什么都不做
                }

                // 等待连接
                delay(1_000)
                val socket = AsyncTcpSocketFd(eventLoop, UnixDomainSocketAddress.of(socketPath.toPath()))
                val connectRes = socket.asyncConnect().await()
                log.info("after connect")
                if (connectRes < 0) {
                    throw SyscallException(connectRes)
                }
                val drop = AtomicBoolean(false)
                var readBuffer = MalloceUnboundMemory(ValueLayout.JAVA_LONG.byteSize()) {
                    drop.set(true)
                    log.info("resource will be dropped")
                }

               val task = async {
                   socket.pollableRead(readBuffer, readBuffer.resource().byteSize().toInt(), 0)
               }

                delay(1_000)
                log.info("cancel!")
                task.cancel()
                delay(5_00)
                Assert.assertTrue(drop.get())

                val jdkSocket =socketRef.get()
                Assert.assertNotNull(jdkSocket)

                val readTask = async {
                    socket.pollableRead(readBuffer, readBuffer.resource().byteSize().toInt(), 0)
                }

                val message = "hello coroutine poll".toByteArray(Charset.defaultCharset())
                readBuffer = MalloceUnboundMemory(message.size.toLong())

                delay(5_00)

                val writeResult = jdkSocket.write(ByteBuffer.wrap(message))
                log.info("write to jdk socket! size {}", writeResult)

                //read socket
                val bufferResult = readTask.await()

                Assert.assertEquals(bufferResult.syscallRes, message.size)
                val buffer = readBuffer.resource()
                val read = buffer.toArray(ValueLayout.JAVA_BYTE)
                log.info("poll read is : {}", String(read))
                Assert.assertArrayEquals(message, read)
            }

        }
    }

    @Test
    fun test(): Unit = runBlocking {
        val task = async {
            someDelay()
        }
        delay(1000)
        task.cancel()
        log.info("cancel !")
        delay(10_000)

    }


    suspend fun someDelay() {
        try {
            withContext(NonCancellable + Dispatchers.Default) {
                delay(2_000)
                log.info("cant cancel")
            }
            log.info("after withContext(NonCancellable) : {}", currentCoroutineContext().job.isCancelled)
        } finally {
            log.info("finally, cancel: {}", currentCoroutineContext().job.isCancelled)
        }
    }

}