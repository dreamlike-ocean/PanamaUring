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

public class VTIoUringTest {

    @Test
    public void testIoUringVT() {
        VTIoUringEventLoop eventLoop = new VTIoUringEventLoop(params -> {
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
        } catch (Exception exception) {
        }
    }
}
