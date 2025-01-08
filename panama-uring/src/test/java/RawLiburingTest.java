import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.OSIoUringProbe;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class RawLiburingTest {

    @Test
    public void testProbe() {
        OSIoUringProbe probe = new OSIoUringProbe();
        int lastOp = probe.getLastOp();
        Assert.assertTrue(lastOp > 0);
        Assert.assertEquals(lastOp + 1, probe.getOps().length);

        MemorySegment ioUringMemory = Instance.LIBC_MALLOC.malloc(IoUring.LAYOUT.byteSize());
        try {

            IoUring ioUring = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringMemory);
            int result = Instance.LIB_URING.io_uring_queue_init(2, ioUring, 0);
            Assert.assertFalse(result < 0);
            Assert.assertEquals(ioUring.getFeatures(), probe.getFeatures());
        } finally {
            Instance.LIBC_MALLOC.free(ioUringMemory);
        }
    }

    @Test
    public void testRegisterEventFd() throws Exception {
        IoUringCore ioUringCore = new IoUringCore(p -> p.setSq_entries(2));
        EventFd readyEventFd = new EventFd(0, Libc.EventFd_H.EFD_NONBLOCK);
        EventFd pollEventFd = new EventFd();
        try {
            int res = Instance.LIB_URING.io_uring_register_eventfd(ioUringCore.getInternalRing(), readyEventFd.fd());
            Assert.assertTrue(res >= 0);

            IoUringSqe sqe = ioUringCore.ioUringGetSqe(true).get();
            MemorySegment readBuffer = Arena.global().allocate(ValueLayout.JAVA_LONG);
            Instance.LIB_URING.io_uring_prep_read(sqe, pollEventFd.fd(), readBuffer, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            pollEventFd.eventfdWrite(1);
            ioUringCore.submit();

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment readyEventReadBuffer = arena.allocate(ValueLayout.JAVA_LONG);
                int i = readyEventFd.eventfdRead(readyEventReadBuffer);
                Assert.assertTrue(i >= 0);
                int count = readyEventReadBuffer.get(ValueLayout.JAVA_INT, 0);
                Assert.assertTrue(count > 0);
            }
        } finally {
            int res = Instance.LIB_URING.io_uring_unregister_eventfd(ioUringCore.getInternalRing());
            Assert.assertTrue(res >= 0);
            ioUringCore.close();
            readyEventFd.close();
            pollEventFd.close();
        }
    }
}
