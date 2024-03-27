import org.junit.Assert;
import org.junit.Test;
import struct.io_uring_cqe_struct;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncEventFd;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.fd.EventFd;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.*;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.FileInputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LiburingTest {


    @Test
    public void testLayout() {
        MemoryLayout sqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class).withName("io_uring_sqe");
        Assert.assertEquals(io_uring_sqe_struct.layout.byteSize(), sqeLayout.byteSize());
        Assert.assertEquals(io_uring_sqe_struct.layout, sqeLayout);


        MemoryLayout cqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCqe.class).withName("io_uring_cqe");
        Assert.assertEquals(io_uring_cqe_struct.layout.byteSize(), cqeLayout.byteSize());
        Assert.assertEquals(io_uring_cqe_struct.layout, cqeLayout);


        VarHandle handle = sqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("flagsUnion"),
                MemoryLayout.PathElement.groupElement("rw_flags")
        );
        MemorySegment memorySegment = Arena.global().allocate(sqeLayout.byteSize());
        handle.set(memorySegment, 0, 100);
        int flag = (int) handle.get(memorySegment, 0);
        IoUringSqe sqe = Instance.STRUCT_PROXY_GENERATOR.enhance(memorySegment);
        int rwFlags = sqe.getFlagsUnion().getRw_flags();
        Assert.assertEquals(flag, rwFlags);
        sqe.setOffset(1024L);
        Assert.assertEquals(1024L, sqe.getOffset());
        Assert.assertEquals(1024L, sqe.getOffsetUnion().getOff());
        sqe.setAddr(2048);
        Assert.assertEquals(2048, sqe.getAddr());
        Assert.assertEquals(2048, sqe.getBufferUnion().getAddr());
        sqe.setFlagsInFlagsUnion(4);
        Assert.assertEquals(4, sqe.getRwFlags());
        Assert.assertEquals(4, sqe.getFlagsUnion().getAccept_flags());
        Assert.assertEquals(4, sqe.getFlagsUnion().getRw_flags());
        sqe.setAddr3(5);
        Assert.assertEquals(5, sqe.getAddr3());
        Assert.assertEquals(5, sqe.getAddr3Union().getAddr3().getAddr3());
        sqe.setPad2(6);
        Assert.assertEquals(6, sqe.getPad2());
        Assert.assertEquals(6, sqe.getAddr3Union().getAddr3().get__pad2().getAtIndex(ValueLayout.JAVA_LONG, 0));


        MemoryLayout ioUringParamsLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringParams.class).withName("io_uring_params");

        Assert.assertEquals(struct.io_uring_params.layout.byteSize(), ioUringParamsLayout.byteSize());
        Assert.assertEquals(struct.io_uring_params.layout, ioUringParamsLayout);


        MemoryLayout ioUringSqLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSq.class).withName("io_uring_sq");
        Assert.assertEquals(struct.io_uring_sq.layout.byteSize(), ioUringSqLayout.byteSize());

        MemoryLayout ioUringCqLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCq.class).withName("io_uring_cq");
        Assert.assertEquals(struct.io_uring_cq.layout.byteSize(), ioUringCqLayout.byteSize());

        MemoryLayout ioUringLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUring.class).withName("io_uring");
        Assert.assertEquals(struct.io_uring.layout.byteSize(), ioUringLayout.byteSize());
    }

    @Test
    public void testAsyncFile() throws Throwable {
        LibUring libUring = Instance.LIB_URING;

        Libc libc = Instance.LIBC;
        File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        tmpFile.deleteOnExit();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment randomMemory = arena.allocate(ValueLayout.JAVA_INT);
            IoUring ioUring = Instance.STRUCT_PROXY_GENERATOR.allocate(arena, IoUring.class);
            String absolutePath = tmpFile.getAbsolutePath();
            MemorySegment pathname = arena.allocateFrom(absolutePath);
            int fd = libc.open(pathname, Libc.Fcntl_H.O_RDWR);
            Assert.assertTrue(fd > 0);
            int initRes = libUring.io_uring_queue_init(4, ioUring, 0);

            Assert.assertEquals(0, initRes);
            String helloIoUring = "hello io_uring";
            MemorySegment str = arena.allocateFrom(helloIoUring);

            IoUringCqe cqeStruct = submitTemplate(arena, ioUring, (sqe) -> {
                libUring.io_uring_prep_write(sqe, fd, str, (int) str.byteSize() - 1, 0);
                sqe.setUser_data(randomMemory.address());
            });
            long userData = cqeStruct.getUser_data();
            Assert.assertEquals(randomMemory.address(), userData);
            libUring.io_uring_cq_advance(ioUring, 1);
            try (FileInputStream stream = new FileInputStream(tmpFile)) {
                String string = new String(stream.readAllBytes());
                Assert.assertEquals(helloIoUring, string);
            }

            MemorySegment cqe = arena.allocate(ValueLayout.ADDRESS);
            int count = libUring.io_uring_peek_batch_cqe(ioUring, cqe, 1);
            Assert.assertEquals(0, count);

            MemorySegment readBuffer = arena.allocate(str.byteSize() - 1);
            cqeStruct = submitTemplate(arena, ioUring, (sqe) -> {
                libUring.io_uring_prep_read(sqe, fd, readBuffer, (int) readBuffer.byteSize(), 0);
                sqe.setUser_data(readBuffer.address());
            });
            Assert.assertTrue(cqeStruct.getRes() > 0);
            Assert.assertEquals(readBuffer.address(), cqeStruct.getUser_data());
            Assert.assertEquals(helloIoUring,DebugHelper.bufToString(readBuffer, cqeStruct.getRes()));
            libUring.io_uring_cq_advance(ioUring, 1);
            libUring.io_uring_queue_exit(ioUring);
        }
    }


    @Test
    public void testAsyncFd() {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        try (eventLoop;
             Arena allocator = Arena.ofConfined()) {
            eventLoop.start();
            AsyncEventFd eventFd = new AsyncEventFd(eventLoop);
            eventFd.eventfdWrite(1);
            OwnershipMemory memory = OwnershipMemory.of(allocator.allocate(ValueLayout.JAVA_LONG));
            CancelableFuture<Integer> read = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), (int)read.get());
            Assert.assertEquals(1, memory.resource().get(ValueLayout.JAVA_LONG,0));

            CancelableFuture<Integer> cancelableReadFuture = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);

            eventLoop.submitScheduleTask(1, TimeUnit.SECONDS, () -> {
                cancelableReadFuture.cancel()
                        .thenAccept(count -> Assert.assertEquals(1L, (long)count));
            });

            Integer res = cancelableReadFuture.get();
            Assert.assertEquals(Libc.Error_H.ECANCELED, -res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IoUringCqe submitTemplate(Arena arena, IoUring uring, Consumer<IoUringSqe> sqeFunction) {
        LibUring libUring = Instance.LIB_URING;
        IoUringSqe sqe = libUring.io_uring_get_sqe(uring);
        sqeFunction.accept(sqe);
        int submits = libUring.io_uring_submit_and_wait(uring, 1);
        Assert.assertTrue(submits > 0);
        MemorySegment cqe = arena.allocate(ValueLayout.ADDRESS);
        int count = libUring.io_uring_peek_batch_cqe(uring, cqe, 1);
        Assert.assertTrue(count > 0);
        return Instance.STRUCT_PROXY_GENERATOR.enhance(cqe.getAtIndex(ValueLayout.ADDRESS, 0));
    }

}
