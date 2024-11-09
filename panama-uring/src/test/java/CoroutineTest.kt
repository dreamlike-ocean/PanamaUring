import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory
import top.dreamlike.panama.generator.proxy.NativeArrayPointer
import top.dreamlike.panama.generator.proxy.StructProxyGenerator
import top.dreamlike.panama.uring.async.fd.AsyncEventFd
import top.dreamlike.panama.uring.coroutine.*
import top.dreamlike.panama.uring.nativelib.Instance
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec
import top.dreamlike.panama.uring.trait.OwnershipResource
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.util.concurrent.CountDownLatch

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
}