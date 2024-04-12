import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufRingSetupResult;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class AdvanceLiburingTest {

    @Test
    public void testSelectedRead() {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop){
            eventLoop.start();
            String hello = "hello new selected read";
            File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            tmpFile.deleteOnExit();
            new FileOutputStream(tmpFile).write(hello.getBytes());

            AsyncFileFd asyncFileFd = new AsyncFileFd(eventLoop, tmpFile);

            IoUringBufRingSetupResult ringSetupResult = eventLoop.setupBufferRing(2, 1024, (short) 1).get();
            IoUringBufferRing bufferRing = ringSetupResult.bufRing();
            Assert.assertNotNull(bufferRing);
            Assert.assertFalse(ringSetupResult.res() < 0);


            boolean hasBind = asyncFileFd.bindBufferRing(bufferRing);
            Assert.assertTrue(hasBind);

            OwnershipMemory memory = asyncFileFd.asyncSelectedRead(1024, 0).get();

            String actual = DebugHelper.bufToString(memory.resource(), (int) memory.resource().byteSize());
            Assert.assertEquals(hello, actual);

            IoUringBufferRingElement ringElement = PanamaUringSecret.lookupOwnershipBufferRingElement.apply(memory);
            int bid = ringElement.bid();
            Assert.assertEquals(bufferRing, ringElement.ring());
            Assert.assertTrue(bufferRing.getMemoryByBid(bid).hasOccupy());
            memory.drop();
            Thread.sleep(500);
            Assert.assertFalse( bufferRing.getMemoryByBid(bid).hasOccupy());

            bufferRing.releaseRing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
