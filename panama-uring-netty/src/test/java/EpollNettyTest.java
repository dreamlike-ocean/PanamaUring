import io.github.dreamlike.panama.uring.netty.Epoll.EpollBridgeEventLoop;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.EpollIoHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class EpollNettyTest {

    @Test
    public void testEpoll() throws IOException, ExecutionException, InterruptedException {
        MultiThreadIoEventLoopGroup mainGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        SingleThreadIoEventLoop ioEventLoop = (SingleThreadIoEventLoop) mainGroup.next();

        EpollBridgeEventLoop epollBridgeEventLoop = new EpollBridgeEventLoop(ioEventLoop, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        tmpFile.deleteOnExit();

        epollBridgeEventLoop.start();

        AsyncFileFd fileFd = new AsyncFileFd(epollBridgeEventLoop, tmpFile);
        Arena allocator = Arena.global();
        var hello = "hello netty and PanamaUring".getBytes(StandardCharsets.UTF_8);
        MemorySegment memorySegment = allocator.allocateFrom(ValueLayout.JAVA_BYTE, hello);
        CancelableFuture<BufferResult<OwnershipMemory>> future = fileFd.asyncWrite(OwnershipMemory.of(memorySegment), (int) memorySegment.byteSize(), 0);

        var writeRes = future
                .get().syscallRes();

        Assertions.assertEquals(hello.length, writeRes);

        MemorySegment readBuffer = allocator.allocate(ValueLayout.JAVA_BYTE, hello.length);
        CancelableFuture<BufferResult<OwnershipMemory>> readFuture = fileFd.asyncRead(OwnershipMemory.of(readBuffer), writeRes, 0);

        var readRes = readFuture
                .get()
                .syscallRes();
        Assertions.assertEquals(writeRes, readRes);
        Assertions.assertArrayEquals(hello, readBuffer.toArray(ValueLayout.JAVA_BYTE));

        mainGroup.close();
    }
}
