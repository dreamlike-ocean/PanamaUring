import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.OSIoUringProbe;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RawLiburingTest {

    private static final Logger log = LoggerFactory.getLogger(RawLiburingTest.class);

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
    public void testSubmit() {
        try (IoUringCore ioUringCore = new IoUringCore(p -> p.setSq_entries(2))) {

            log.info("start wait");
            int res = ioUringCore.submitAndWait(TimeUnit.MILLISECONDS.toNanos(500));
            log.info("end wait");
            //todo 为何只返回-1？
//             Assert.assertEquals(-Libc.Error_H.ETIME, res);

            long userData = 20240214;
            prepNoop(ioUringCore, userData);

            ioUringCore.submit();

            Assert.assertEquals(1, ioUringCore.countReadyCqe());

            List<IoUringCqe> ioUringCqes = ioUringCore.processCqes();
            Assert.assertEquals(1, ioUringCqes.size());
            IoUringCqe ioUringCqe = ioUringCqes.get(0);
            Assert.assertEquals(userData, ioUringCqe.getUser_data());
            ioUringCore.ioUringCqAdvance(1);

            EventFd eventFd = new EventFd(0, Libc.EventFd_H.EFD_NONBLOCK);
            var ioUringSqe = ioUringCore.ioUringGetSqe(true).get();
            MemorySegment readBuffer = Arena.global().allocate(ValueLayout.JAVA_LONG);
            Instance.LIB_URING.io_uring_prep_read(ioUringSqe, eventFd.fd(), readBuffer, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            long start = System.currentTimeMillis();
            ioUringCore.submitAndWait(TimeUnit.MILLISECONDS.toNanos(500));
            log.info("wait :{}", System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void prepNoop(IoUringCore ioUringCore, long userData) {
        IoUringSqe ioUringSqe = ioUringCore.ioUringGetSqe(true).get();
        Instance.LIB_URING.io_uring_prep_nop(ioUringSqe);
        ioUringSqe.setUser_data(userData);
    }

    private EventFd prepReadEventFd(IoUringCore ioUringCore) {
        EventFd eventFd = new EventFd();

        MemorySegment readBuffer = Instance.LIBC_MALLOC.malloc(8);

        IoUringSqe ioUringSqe = ioUringCore.ioUringGetSqe(true).get();

        Instance.LIB_URING.io_uring_prep_read(ioUringSqe, eventFd.fd(), readBuffer, 8, 0);

        return eventFd;
    }

    @Test
    public void testRegisterEventFd() throws Exception {
        IoUringCore ioUringCore = new IoUringCore(p -> p.setSq_entries(2));
        EventFd readyEventFd = new EventFd(0, Libc.EventFd_H.EFD_NONBLOCK);
        EventFd pollEventFd = new EventFd();
        try {

            int registerRingFdResult = Instance.LIB_URING.io_uring_register_ring_fd(ioUringCore.getInternalRing());
            Assert.assertEquals(1, registerRingFdResult);


            int res = Instance.LIB_URING.io_uring_register_eventfd(ioUringCore.getInternalRing(), readyEventFd.fd());
            Assert.assertTrue(res >= 0);

            IoUringSqe sqe = ioUringCore.ioUringGetSqe(true).get();
            MemorySegment readBuffer = Arena.global().allocate(ValueLayout.JAVA_LONG);
            Instance.LIB_URING.io_uring_prep_read(sqe, pollEventFd.fd(), readBuffer, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            pollEventFd.eventfdWrite(1);
            ioUringCore.submit();

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment readyEventReadBuffer = arena.allocate(ValueLayout.JAVA_LONG);

                MemorySegment files = arena.allocate(ValueLayout.JAVA_INT, 1);
                files.set(ValueLayout.JAVA_INT, 0, readyEventFd.fd());
                int registerFiles = Instance.LIB_URING.io_uring_register_files(ioUringCore.getInternalRing(), files, 1);
                Assert.assertTrue(registerFiles >= 0);

                MemorySegment iovec = arena.allocate(Iovec.LAYOUT, 1);
                NativeArrayPointer<Iovec> iovecNativeArray = new NativeArrayPointer<>(Instance.STRUCT_PROXY_GENERATOR, iovec);
                iovecNativeArray.getAtIndex(0).setIov_base(readyEventReadBuffer);
                iovecNativeArray.getAtIndex(0).setIov_len(readyEventReadBuffer.byteSize());

                int registerBuffer = Instance.LIB_URING.io_uring_register_buffers(ioUringCore.getInternalRing(), iovecNativeArray, 1);
                Assert.assertTrue(registerBuffer >= 0);

                int i = readyEventFd.eventfdRead(readyEventReadBuffer);
                Assert.assertTrue(i >= 0);
                int count = readyEventReadBuffer.get(ValueLayout.JAVA_INT, 0);
                Assert.assertTrue(count > 0);
            }
        } finally {
            int res = Instance.LIB_URING.io_uring_unregister_eventfd(ioUringCore.getInternalRing());
            Assert.assertTrue(res >= 0);

            int unRegisterFiles = Instance.LIB_URING.io_uring_unregister_files(ioUringCore.getInternalRing());
            Assert.assertTrue(unRegisterFiles >= 0);

            int unregisterBuffers = Instance.LIB_URING.io_uring_unregister_buffers(ioUringCore.getInternalRing());
            Assert.assertTrue(unregisterBuffers >= 0);

            int i = Instance.LIB_URING.io_uring_unregister_ring_fd(ioUringCore.getInternalRing());
            Assert.assertEquals(1, i);
            ioUringCore.close();
            readyEventFd.close();
            pollEventFd.close();
        }
    }

    @Test
    public void testRegister() {
        for (Method method : LibUring.class.getMethods()) {
            if (!method.isDefault()) {
                System.out.println(method.getName());
            }
        }
    }

}
