import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.sync.fd.EventFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IoUringPlayground {

    private static final MethodHandle readMH;
    private static final Logger log = LoggerFactory.getLogger(IoUringPlayground.class);

    static {
        MemorySegment fp = Linker.nativeLinker()
                .defaultLookup()
                .find("read").get();
        readMH = Linker.nativeLinker()
                .downcallHandle(
                        fp,
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                        Linker.Option.captureCallState("errno")
                );

    }

    @Test
    public void testBlockSocket() throws Exception {
        EventFd eventFd = new EventFd(0, Libc.Fcntl_H.O_NONBLOCK);

        try (IoUringCore ioUringCore = new IoUringCore(p -> {
            p.setSq_entries(4);
            p.setFlags(0);
        })) {

            int errno = readNonBlock(eventFd.readFd());
            Assert.assertEquals(Libc.Error_H.EAGAIN, errno);

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
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testNonBlockSocket() throws Throwable {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9090);
        InetAddress inetAddress = address.getAddress();
        Thread.startVirtualThread(() -> {
            try {
                ServerSocket socket = new ServerSocket(address.getPort(), 50, inetAddress);
                Socket _ = socket.accept();
                log.info("accept,but dont handle");
            } catch (IOException e) {
                log.error("error", e);
                throw new RuntimeException(e);
            }
        });

        int socketFd = NativeHelper.socketSysCall(address);

        OwnershipMemory remoteAddr = NativeHelper.mallocAddr(address);
        MemorySegment addr = remoteAddr.resource();
        int connectRes = Instance.LIBC.connect(socketFd, addr, (int) addr.byteSize());
        Assert.assertEquals(0, connectRes);
        NativeHelper.makeNonBlock(socketFd);
        int errorNo = readNonBlock(socketFd);
        Assert.assertEquals(Libc.Error_H.EAGAIN, errorNo);

        try (IoUringCore ioUringCore = new IoUringCore(p -> {
            p.setSq_entries(4);
            p.setFlags(0);
        })) {
            IoUringSqe ioUringSqe = ioUringCore.ioUringGetSqe(true).get();
            Instance.LIB_URING.io_uring_prep_recv(ioUringSqe, socketFd, Arena.ofAuto().allocate(ValueLayout.JAVA_LONG), 8, Libc.Socket_H.Flag.MSG_DONTWAIT);

            ioUringCore.submitAndWait(-1);
            List<IoUringCqe> ioUringCqes = ioUringCore.processCqes();
            Assert.assertEquals(1, ioUringCqes.size());
            IoUringCqe ioUringCqe = ioUringCqes.get(0);
            Assert.assertEquals(-Libc.Error_H.EAGAIN, ioUringCqe.getRes());
        }
    }


    public int readNonBlock(int fd) throws Throwable {
        StructLayout capturedStateLayout = Linker.Option.captureStateLayout();
        VarHandle errnoHandle = capturedStateLayout.varHandle(MemoryLayout.PathElement.groupElement("errno"));
        MemorySegment capturedState = Arena.global().allocate(capturedStateLayout);
        readMH.invoke(capturedState, fd, Arena.global().allocate(ValueLayout.JAVA_LONG), 8);
        int errno = (int) errnoHandle.get(capturedState, 0L);
        return errno;
    }


    @Test
    public void overflowTest() throws Exception {
        int sqeCount = 4;
        try (IoUringCore ioUringCore = new IoUringCore(p -> {
            p.setSq_entries(sqeCount);
            p.setCq_entries(sqeCount);
            p.setFlags(IoUringConstant.IORING_SETUP_CQSIZE);
        })) {
            if ((ioUringCore.getInternalRing().getFeatures() & IoUringConstant.IORING_FEAT_NODROP) == 0) {
                log.info("current kernel dont support IORING_FEAT_NODROP");
                return;
            }

            for (int i = 0; i < sqeCount; i++) {
                IoUringSqe ioUringSqe = ioUringCore.ioUringGetSqe(true).get();
                Instance.LIB_URING.io_uring_prep_nop(ioUringSqe);
            }

            int submitResult = ioUringCore.submit();
            Assert.assertEquals(sqeCount, submitResult);
            Assert.assertEquals(sqeCount,ioUringCore.countReadyCqe());

            for (int i = 0; i < sqeCount; i++) {
                IoUringSqe ioUringSqe = ioUringCore.ioUringGetSqe(true).get();
                Instance.LIB_URING.io_uring_prep_nop(ioUringSqe);
            }
            submitResult = ioUringCore.submit();
            Assert.assertEquals(sqeCount, submitResult);

            AtomicInteger counter = new AtomicInteger(0);
            while (true) {
                int old = counter.get();
                ioUringCore.processCqes(cqe -> {
                    counter.incrementAndGet();
                });
                if (old == counter.get()) {
                    break;
                }
            }

            MemorySegment memorySegment = StructProxyGenerator.findMemorySegment(ioUringCore.getInternalRing());
            int kOverflow = (int) IoUringConstant.AccessShortcuts.IO_URING_CQ_K_OVERFLOW_VARHANDLE.get(memorySegment, 0L);
            //todo k overflow is not work
            Assert.assertEquals(sqeCount * 2, counter.get());
            Assert.assertEquals(0,ioUringCore.countReadyCqe());
        }
    }
}