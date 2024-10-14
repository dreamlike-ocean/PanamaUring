import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.sync.fd.EventFd;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

public class IoUringPlayground {

    @Test
    public void testBlockSocket() throws Exception {
        EventFd eventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);

        try (IoUringCore ioUringCore = new IoUringCore(p -> {
            p.setSq_entries(4);
            p.setFlags(0);
        })){

            IoUringSqe sqe = ioUringCore.ioUringGetSqe(true).get();
            MemorySegment readBuffer = Arena.global().allocate(ValueLayout.JAVA_LONG);
            Instance.LIB_URING.io_uring_prep_read(sqe, eventFd.fd(), readBuffer, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            eventFd.eventfdWrite(2);
            ioUringCore.submitAndWait(-1);
            List<IoUringCqe> uringCqes = ioUringCore.processCqes();
            Assert.assertEquals(1, uringCqes.size());
            IoUringCqe cqe = uringCqes.get(0);

            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), cqe.getRes());
            Assert.assertEquals(2, readBuffer.get(ValueLayout.JAVA_LONG, 0));

            int i = Instance.LIB_URING.io_uring_submit_and_wait(ioUringCore.getInternalRing(), 1);
            Assert.assertEquals(0, i);
            IoUringCqe[] holder = new IoUringCqe[1];
            ioUringCore.processCqes(c -> {
                holder[0] = c;
            }, true);

            cqe = holder[0];
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), cqe.getRes());

            i = Instance.LIB_URING.io_uring_submit_and_wait(ioUringCore.getInternalRing(), 0);
            Assert.assertEquals(0, i);
        }

    }
}
