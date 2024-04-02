import org.junit.Assert;
import org.junit.Test;
import struct.io_uring_cqe_struct;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.async.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncEventFd;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.async.fd.AsyncTcpServerSocketFd;
import top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.*;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.io.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
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

        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        File tmpFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        tmpFile.deleteOnExit();
        try (Arena arena = Arena.ofConfined();
             eventLoop) {
            eventLoop.start();
            String absolutePath = tmpFile.getAbsolutePath();
            MemorySegment pathname = arena.allocateFrom(absolutePath);
            OwnershipMemoryForTest memory = new OwnershipMemoryForTest(pathname);
            AsyncFileFd fd = AsyncFileFd.asyncOpen(eventLoop, memory, Libc.Fcntl_H.O_RDWR).get();
            Assert.assertTrue(memory.hasDrop);
            String helloIoUring = "hello io_uring";
            MemorySegment str = arena.allocateFrom(helloIoUring);
            //async write
            OwnershipMemoryForTest writeBuffer = new OwnershipMemoryForTest(str);
            Integer writeRes = fd.asyncWrite(writeBuffer, (int) str.byteSize() - 1, 0).get().syscallRes();
            Assert.assertTrue(writeRes > 0);
            try (FileInputStream stream = new FileInputStream(tmpFile)) {
                String string = new String(stream.readAllBytes());
                Assert.assertEquals(helloIoUring, string);
            }
            Integer fsyncRes = fd.asyncFsync(0).get();
            Assert.assertTrue(fsyncRes == 0);
            fsyncRes = fd.asyncFsync(0, 0, (int) (str.byteSize() - 1)).get();
            Assert.assertTrue(fsyncRes == 0);
            //async read
            var readBuffer = new OwnershipMemoryForTest(arena.allocate(str.byteSize() - 1));
            Integer readRes = fd.asyncRead(readBuffer, (int) str.byteSize() - 1, 0).get().syscallRes();
            Assert.assertTrue(readRes > 0);
            Assert.assertEquals(helloIoUring, DebugHelper.bufToString(readBuffer.resource(), readRes));
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
            //readTest
            eventLoop.start();
            AsyncEventFd eventFd = new AsyncEventFd(eventLoop);
            eventFd.eventfdWrite(1);
            OwnershipMemory memory = OwnershipMemory.of(allocator.allocate(ValueLayout.JAVA_LONG));
            var read = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), (int) read.get().syscallRes());
            Assert.assertEquals(1, memory.resource().get(ValueLayout.JAVA_LONG, 0));

            //cancel Test:
            var cancelableReadFuture = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            eventLoop.submitScheduleTask(1, TimeUnit.SECONDS, () -> {
                cancelableReadFuture.cancel()
                        .thenAccept(count -> Assert.assertEquals(1L, (long) count));
            });
            Integer res = cancelableReadFuture.get().syscallRes();
            Assert.assertEquals(Libc.Error_H.ECANCELED, -res);

            eventFd.eventfdWrite(214);
            //readVTest
            Iovec iovec = Instance.STRUCT_PROXY_GENERATOR.allocate(allocator, Iovec.class);
            MemorySegment readBufferVec0 = allocator.allocate(ValueLayout.JAVA_LONG);
            iovec.setIov_base(readBufferVec0);
            iovec.setIov_len(ValueLayout.JAVA_LONG.byteSize());
            NativeArrayPointer<Iovec> pointer = new NativeArrayPointer<>(Instance.STRUCT_PROXY_GENERATOR, StructProxyGenerator.findMemorySegment(iovec), Iovec.class);
            OwnershipResourceForTest<NativeArrayPointer<Iovec>> resource = new OwnershipResourceForTest<>(pointer);
            var readvRes = eventFd.asyncReadV(resource, 1, 0)
                    .get();
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), (int) readvRes.syscallRes());
            Assert.assertEquals(214, readBufferVec0.get(ValueLayout.JAVA_LONG, 0));
            Assert.assertTrue(resource.dontDrop());

            //writeVTest
            resource = new OwnershipResourceForTest<>(pointer);
            readBufferVec0.set(ValueLayout.JAVA_LONG, 0, 329);
            Integer writeVRes = eventFd.asyncWriteV(resource, 1, 0).get().syscallRes();
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), (int) writeVRes);
            var assertReadBuffer = allocator.allocate(ValueLayout.JAVA_LONG);
            eventFd.read(assertReadBuffer, (int) ValueLayout.JAVA_LONG.byteSize());
            Assert.assertEquals(329, assertReadBuffer.get(ValueLayout.JAVA_LONG, 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBuffRing() {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
            eventLoop.start();
            int NREQS = 64;
            int BUF_SIZE = 1024;
            short bufferGroup = 1;
            int BR_MASK = Instance.LIB_URING.io_uring_buf_ring_mask(NREQS);

            IoUringBufRingSetupResult result = eventLoop.setupBufferRing(NREQS, bufferGroup).get();
            Assert.assertEquals(0, result.res());
            IoUringBufRing bufRing = result.bufRing();
            Assert.assertNotNull(bufRing);
            Assert.assertEquals(bufRing.getOwner(), eventLoop);
            Assert.assertNotEquals(MemorySegment.NULL, StructProxyGenerator.findMemorySegment(bufRing));
            Assert.assertThrows(IllegalStateException.class, () -> {
                bufRing.ioUringBufRingAdd(MemorySegment.NULL, BUF_SIZE, (short) 0, 0, 0);
            });
            int totalLen = NREQS * BUF_SIZE;
            try (OwnershipMemory bufPtrPtr = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.ADDRESS.byteSize());
                 OwnershipMemory bufBase = Instance.LIB_JEMALLOC.posixMemalign(bufPtrPtr.resource(), 4096, totalLen);
            ) {
                CompletableFuture.runAsync(() -> {
                    MemorySegment base = bufBase.resource();
                    for (int i = 0; i < NREQS; i++) {
                        bufRing.ioUringBufRingAdd(base, BUF_SIZE, (short) (i + 1), BR_MASK, i);
                        base = MemorySegment.ofAddress(base.address() + BUF_SIZE);
                    }
                    bufRing.ioUringBufRingAdvance(NREQS);

                }, eventLoop).get();
                AsyncEventFd eventFd = new AsyncEventFd(eventLoop);
                eventFd.eventfdWrite(214);

                IoUringCqe cqe = eventFd.asyncSelectedRead((int) ValueLayout.JAVA_LONG.byteSize(), 0, bufferGroup).get();
                Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), cqe.getRes());
                int bid = cqe.getFlags() >> IoUringConstant.IORING_CQE_BUFFER_SHIFT;
                MemorySegment base = bufBase.resource();
                base = MemorySegment.ofAddress(base.address() + (bid - 1) * BUF_SIZE).reinterpret(BUF_SIZE);
                Assert.assertEquals(214, base.get(ValueLayout.JAVA_INT, 0));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testAsyncServer() throws Exception {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
            eventLoop.start();
            AsyncTcpServerSocketFd serverFd = new AsyncTcpServerSocketFd(eventLoop, new InetSocketAddress("127.0.0.1", 4399), 4399);
            serverFd.bind();
            serverFd.listen(16);

            long addrSize = serverFd.addrSize();
            OwnershipMemoryForTest addr = new OwnershipMemoryForTest(Instance.LIB_JEMALLOC.malloc(addrSize));
            OwnershipMemoryForTest addrLen = new OwnershipMemoryForTest(Instance.LIB_JEMALLOC.malloc(ValueLayout.JAVA_INT.byteSize()));

            CancelableFuture<AsyncTcpSocketFd> waitAccept = serverFd.asyncAccept(0, addr, addrLen);
            ArrayBlockingQueue<Socket> oneshot = new ArrayBlockingQueue<>(1);
            Thread.startVirtualThread(() -> {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress("127.0.0.1", 4399));
                    oneshot.put(socket);
                    socket.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
            AsyncTcpSocketFd asyncTcpSocketFd = waitAccept.get();
            Socket clientSocket = oneshot.take();
            Assert.assertNotNull(clientSocket);
            Assert.assertEquals(clientSocket.getLocalPort(), ((InetSocketAddress) asyncTcpSocketFd.getRemoteAddress()).getPort());
            Assert.assertFalse(addr.dontDrop());
            Assert.assertFalse(addrLen.dontDrop());
            serverFd.close();
        }
    }

    @Test
    public void testAsyncSocket() throws Exception {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        int randomPort = 8083;
        try (eventLoop; Arena allocator = Arena.ofConfined()) {
            eventLoop.start();
            ArrayBlockingQueue<Socket> queue = new ArrayBlockingQueue<>(1);
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", randomPort);
            Thread.startVirtualThread(() -> {
                try {
                    ServerSocket socket = new ServerSocket(address.getPort(), 4, InetAddress.getByName(address.getHostName()));
                    socket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
                    Socket accept = socket.accept();
                    queue.offer(accept);
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
            //睡一秒得了 等上面的启动
            Thread.sleep(1_000);
            AsyncTcpSocketFd tcpSocket = new AsyncTcpSocketFd(eventLoop, address);
            //connect
            Integer connectSyscallRes = tcpSocket.asyncConnect().get();
            if (connectSyscallRes != 0) {
                System.out.println(DebugHelper.getErrorStr(-connectSyscallRes));
            }
            Assert.assertEquals(0, (int) connectSyscallRes);
            Socket socket = queue.take();
            int localPort = ((InetSocketAddress) tcpSocket.getLocalAddress()).getPort();
            Assert.assertEquals(socket.getPort(), localPort);
            int port = ((InetSocketAddress) tcpSocket.getRemoteAddress()).getPort();
            Assert.assertEquals(randomPort, port);
            //send
            MemorySegment sendBuffer = allocator.allocateFrom(ValueLayout.JAVA_INT, 214);
            OwnershipMemoryForTest ownershipMemoryForTest = new OwnershipMemoryForTest(sendBuffer);
            Integer sendRes = tcpSocket.asyncSend(ownershipMemoryForTest, (int) sendBuffer.byteSize(), 0).get().syscallRes();
            Assert.assertEquals((int) sendBuffer.byteSize(), (int) sendRes);
            Assert.assertEquals(214, DebugHelper.ntohl(new DataInputStream(socket.getInputStream()).readInt()));
            Assert.assertTrue(ownershipMemoryForTest.dontDrop());
            //recv
            new DataOutputStream(socket.getOutputStream()).writeInt(329);
            MemorySegment recvBuffer = allocator.allocate(ValueLayout.JAVA_INT);
            ownershipMemoryForTest = new OwnershipMemoryForTest(recvBuffer);
            tcpSocket.asyncRecv(ownershipMemoryForTest, (int) recvBuffer.byteSize(), 0).get();
            Assert.assertEquals(329, DebugHelper.ntohl(recvBuffer.get(ValueLayout.JAVA_INT, 0)));
            Assert.assertTrue(ownershipMemoryForTest.dontDrop());
            //recv selected
            int NREQS = 2;
            int BUF_SIZE = 1024;
            int totalLen = NREQS * BUF_SIZE;
            short bufferGroup = 2;
            int BR_MASK = Instance.LIB_URING.io_uring_buf_ring_mask(NREQS);
            IoUringBufRingSetupResult result = eventLoop.setupBufferRing(NREQS, bufferGroup).get();
            Assert.assertEquals(0, result.res());

            new DataOutputStream(socket.getOutputStream()).writeInt(2001);
            OwnershipMemory bufPtrPtr = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.ADDRESS.byteSize());
            OwnershipMemory bufBase = Instance.LIB_JEMALLOC.posixMemalign(bufPtrPtr.resource(), 4096, totalLen);
            CompletableFuture.runAsync(() -> {
                MemorySegment base = bufBase.resource();
                for (int i = 0; i < NREQS; i++) {
                    result.bufRing().ioUringBufRingAdd(base, BUF_SIZE, (short) (i + 1), BR_MASK, i);
                    base = MemorySegment.ofAddress(base.address() + BUF_SIZE);
                }
                result.bufRing().ioUringBufRingAdvance(NREQS);

            }, eventLoop).get();
            IoUringCqe cqe = tcpSocket.asyncRecvSelected(((int) recvBuffer.byteSize()), 0, bufferGroup).get();
            int bid = cqe.getBid();
            MemorySegment base = bufBase.resource();
            base = MemorySegment.ofAddress(base.address() + (long) (bid - 1) * BUF_SIZE).reinterpret(BUF_SIZE);
            Assert.assertEquals(2001, DebugHelper.ntohl(base.get(ValueLayout.JAVA_INT, 0)));

            //sendzc
            ArrayBlockingQueue<Integer> oneshot = new ArrayBlockingQueue<>(1);
            ownershipMemoryForTest = new OwnershipMemoryForTest(sendBuffer);
            Thread.startVirtualThread(() -> {
                try {
                    int targetValue = DebugHelper.ntohl(new DataInputStream(socket.getInputStream()).readInt());
                    oneshot.put(targetValue);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            tcpSocket.asyncSendZc(ownershipMemoryForTest, (int) sendBuffer.byteSize(), 0, 0).get();
            Assert.assertTrue(ownershipMemoryForTest.dontDrop());
            Assert.assertEquals(Integer.valueOf(214), oneshot.take());
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


    private static class OwnershipResourceForTest<T> implements OwnershipResource<T> {
        private final T t;

        private volatile boolean hasDrop = false;

        public OwnershipResourceForTest(T t) {
            this.t = t;
        }

        @Override
        public T resource() {
            return t;
        }

        @Override
        public void drop() {
            hasDrop = true;
        }

        public boolean dontDrop() {
            return !hasDrop;
        }
    }

    private static class OwnershipMemoryForTest implements OwnershipMemory {
        private final MemorySegment memorySegment;

        private volatile boolean hasDrop = false;


        public OwnershipMemoryForTest(MemorySegment memorySegment) {
            this.memorySegment = memorySegment;
        }

        @Override
        public MemorySegment resource() {
            return memorySegment;
        }

        @Override
        public void drop() {
            hasDrop = true;
        }

        public boolean dontDrop() {
            return !hasDrop;
        }

    }

}