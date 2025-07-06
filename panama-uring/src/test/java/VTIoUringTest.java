import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.eventloop.VTIoUringEventLoop;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.FileInputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class VTIoUringTest {

    @Test
    public void testIoUringVT() {
        VTIoUringEventLoop eventLoop = VTIoUringEventLoop.newInstance(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (VTIoUringEventLoop uringEventLoop = eventLoop) {
            Arena arena = Arena.global();
            uringEventLoop.start();
            Assert.assertTrue(uringEventLoop.isVirtual());

            Thread.sleep(1000);
            File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            tmpFile.deleteOnExit();

            AsyncFileFd fd = new AsyncFileFd(eventLoop,tmpFile);
            String helloIoUring = "hello io_uring_vt";

            MemorySegment str = arena.allocateFrom(helloIoUring);
            var writeRes = fd.asyncWrite(OwnershipMemory.of(str), (int) str.byteSize() - 1, 0).get().syscallRes();
            Assert.assertTrue(writeRes > 0);
            try (FileInputStream stream = new FileInputStream(tmpFile)) {
                String string = new String(stream.readAllBytes());
                Assert.assertEquals(helloIoUring, string);
            }

            ThreadFactory vtScheduler = eventLoop.asVTScheduler();
            ExecutorService executor = Executors.newThreadPerTaskExecutor(vtScheduler);
            Thread vtCarrier = CompletableFuture.supplyAsync(VTIoUringEventLoop.LoomSupport::carrierThread, executor).join();
            Assert.assertFalse(vtCarrier.isVirtual());

            Thread ioCarrier = eventLoop.runOnEventLoop(VTIoUringEventLoop.LoomSupport::carrierThread).join();
            Assert.assertSame(ioCarrier, vtCarrier);

            Thread vt = CompletableFuture.supplyAsync(Thread::currentThread, executor).join();
            Thread ioVT = eventLoop.runOnEventLoop(Thread::currentThread).join();
            Assert.assertTrue(ioVT.isVirtual());
            Assert.assertNotSame(vt, ioVT);
        } catch (Exception exception) {
        }
    }
}
