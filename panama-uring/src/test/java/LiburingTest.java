import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import struct.io_uring_cqe_struct;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.async.AsyncPoller;
import top.dreamlike.panama.uring.async.BufferResult;
import top.dreamlike.panama.uring.async.IoUringSyscallOwnershipResult;
import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncEventFd;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpServerSocketFd;
import top.dreamlike.panama.uring.async.fd.AsyncPipeFd;
import top.dreamlike.panama.uring.async.fd.AsyncSplicer;
import top.dreamlike.panama.uring.async.fd.AsyncTcpServerSocketFd;
import top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd;
import top.dreamlike.panama.uring.async.other.IoUringMadvise;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibMman;
import top.dreamlike.panama.uring.nativelib.libs.LibPoll;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCq;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSq;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.sync.fd.PipeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
public class LiburingTest {

    private final static Logger log = LoggerFactory.getLogger(LiburingTest.class);
    public final IoUringEventLoopGetter.EventLoopType eventLoopType;

    public LiburingTest(IoUringEventLoopGetter.EventLoopType eventLoopType) {
        this.eventLoopType = eventLoopType;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return IoUringEventLoopGetter.EventLoopType.values();
    }

    @Test
    public void testLayout() {
        MemoryLayout sqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class).withName("io_uring_sqe");
        Assert.assertEquals(Instance.LIB_URING.io_uring_sqe_struct_size(), sqeLayout.byteSize());
        Assert.assertEquals(io_uring_sqe_struct.layout, sqeLayout);


        MemoryLayout cqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCqe.class).withName("io_uring_cqe");
        Assert.assertEquals(Instance.LIB_URING.io_uring_cqe_struct_size(), cqeLayout.byteSize());
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
        Assert.assertEquals(Instance.LIB_URING.io_uring_sq_struct_size(), ioUringSqLayout.byteSize());

        MemoryLayout ioUringCqLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCq.class).withName("io_uring_cq");
        Assert.assertEquals(Instance.LIB_URING.io_uring_cq_struct_size(), ioUringCqLayout.byteSize());

        MemoryLayout ioUringLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUring.class).withName("io_uring");
        Assert.assertEquals(Instance.LIB_URING.io_uring_struct_size(), ioUringLayout.byteSize());
    }

    @Test
    public void testAsyncFile() throws Throwable {

        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
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
            Assert.assertEquals(0, (int) fsyncRes);
            fsyncRes = fd.asyncFsync(0, 0, (int) (str.byteSize() - 1)).get();
            Assert.assertEquals(0, (int) fsyncRes);
            //async read
            var readBuffer = new OwnershipMemoryForTest(arena.allocate(str.byteSize() - 1));
            Integer readRes = fd.asyncRead(readBuffer, (int) str.byteSize() - 1, 0).get().syscallRes();
            Assert.assertTrue(readRes > 0);
            Assert.assertEquals(helloIoUring, NativeHelper.bufToString(readBuffer.resource(), readRes));
        }
    }


    @Test
    public void testAsyncFd() {
        int cqeSize = 16;
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setCq_entries(cqeSize);
            params.setFlags(IoUringConstant.IORING_SETUP_CQSIZE);
        });

        try (eventLoop;
             Arena allocator = Arena.ofConfined()) {
            //readTest
            eventLoop.start();
            Assert.assertEquals(Integer.valueOf(cqeSize), PanamaUringSecret.getCqSize.apply(eventLoop));
            AsyncEventFd eventFd = new AsyncEventFd(eventLoop);
            eventFd.eventfdWrite(1);
            OwnershipMemory memory = OwnershipMemory.of(allocator.allocate(ValueLayout.JAVA_LONG));
            var read = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), read.get().syscallRes());
            Assert.assertEquals(1, memory.resource().get(ValueLayout.JAVA_LONG, 0));

            //cancel Test:
            var cancelableReadFuture = eventFd.asyncRead(memory, (int) ValueLayout.JAVA_LONG.byteSize(), 0);
            eventLoop.submitScheduleTask(1, TimeUnit.SECONDS, () -> {
                cancelableReadFuture.ioUringCancel(true)
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
            Assert.assertEquals(ValueLayout.JAVA_LONG.byteSize(), readvRes.syscallRes());
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
    public void testAsyncServer() throws Exception {
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
            eventLoop.start();
            AsyncTcpServerSocketFd serverFd = new AsyncTcpServerSocketFd(eventLoop, new InetSocketAddress("127.0.0.1", 4399), 4399);
            serverFd.bind();
            serverFd.listen(16);

            long addrSize = serverFd.addrSize();
            OwnershipMemoryForTest addr = new OwnershipMemoryForTest(Instance.LIBC_MALLOC.malloc(addrSize));
            OwnershipMemoryForTest addrLen = new OwnershipMemoryForTest(Instance.LIBC_MALLOC.malloc(ValueLayout.JAVA_INT.byteSize()));

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
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
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
                System.out.println(NativeHelper.getErrorStr(-connectSyscallRes));
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
            Assert.assertEquals(214, NativeHelper.ntohl(new DataInputStream(socket.getInputStream()).readInt()));
            Assert.assertTrue(ownershipMemoryForTest.dontDrop());
            //recv
            new DataOutputStream(socket.getOutputStream()).writeInt(329);
            MemorySegment recvBuffer = allocator.allocate(ValueLayout.JAVA_INT);
            ownershipMemoryForTest = new OwnershipMemoryForTest(recvBuffer);
            tcpSocket.asyncRecv(ownershipMemoryForTest, (int) recvBuffer.byteSize(), 0).get();
            Assert.assertEquals(329, NativeHelper.ntohl(recvBuffer.get(ValueLayout.JAVA_INT, 0)));
            Assert.assertTrue(ownershipMemoryForTest.dontDrop());

            //sendzc
            ArrayBlockingQueue<Integer> oneshot = new ArrayBlockingQueue<>(1);
            ownershipMemoryForTest = new OwnershipMemoryForTest(sendBuffer);
            Thread.startVirtualThread(() -> {
                try {
                    int targetValue = NativeHelper.ntohl(new DataInputStream(socket.getInputStream()).readInt());
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

    @Test
    public void testAsyncPoller() {
        try (IoUringEventLoop ioUringEventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        })) {
            ioUringEventLoop.start();

            AsyncPipeFd pipeFd = new AsyncPipeFd(ioUringEventLoop);
            log.info("pipeFd: {}", pipeFd);
            String demoStr = "hello async pipe";
            OwnershipMemory writeBuffer = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(demoStr.length());
            MemorySegment.copy(demoStr.getBytes(), 0, writeBuffer.resource(), ValueLayout.JAVA_BYTE, 0, demoStr.length());
            BufferResult<OwnershipMemory> _ = pipeFd.asyncWrite(writeBuffer, demoStr.length(), 0).get();

            OwnershipMemory readBuffer = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(demoStr.length());
            BufferResult<OwnershipMemory> readRes = pipeFd.asyncRead(readBuffer, demoStr.length(), 0).get();
            Assert.assertEquals(demoStr.length(), readRes.syscallRes());
            Assert.assertEquals(demoStr, NativeHelper.bufToString(readBuffer.resource(), demoStr.length()));

            log.info("start test async poller");
            AsyncPoller poller = new AsyncPoller(ioUringEventLoop);
            int syscallRes = Instance.LIBC.fcntl(pipeFd.readFd(), Libc.Fcntl_H.F_SETFL, Libc.Fcntl_H.O_NONBLOCK);
            Assert.assertTrue(syscallRes >= 0);
            int readCount = pipeFd.read(readBuffer.resource(), 0);
            Assert.assertEquals(0, readCount);

            CancelableFuture<Integer> pollInRes = poller.register(pipeFd, LibPoll.POLLIN);
            ioUringEventLoop.submitScheduleTask(1000, TimeUnit.MILLISECONDS, () -> {
                OwnershipMemory writeBuffer1 = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(demoStr.length());
                MemorySegment.copy(demoStr.getBytes(), 0, writeBuffer1.resource(), ValueLayout.JAVA_BYTE, 0, demoStr.length());
                log.info("submitScheduleTask start!");
                pipeFd.asyncWrite(writeBuffer1, demoStr.length(), 0)
                        .thenAccept(br -> {
                            br.buffer().drop();
                            Assert.assertEquals(demoStr.length(), br.syscallRes());
                        });
            });
            Integer pollRes = pollInRes.get();
            Assert.assertTrue((pollRes & LibPoll.POLLIN) != 0);

            MemorySegment syncReadBuffer = Instance.LIBC_MALLOC.malloc(demoStr.length());
            int read = pipeFd.read(syncReadBuffer, demoStr.length());
            Assert.assertEquals(demoStr.length(), read);
            Assert.assertEquals(demoStr, NativeHelper.bufToString(syncReadBuffer, demoStr.length()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testAsyncSplicer() {
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        Arena arena = Arena.ofConfined();
        try (eventLoop; arena) {
            eventLoop.start();
            String sampleString = "helllo splicer!";
            AsyncSplicer splicer = new AsyncSplicer(eventLoop);
            PipeFd pipeFd = new PipeFd();

            File readFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            readFile.deleteOnExit();
            File writeFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            writeFile.deleteOnExit();
            new FileOutputStream(readFile).write(sampleString.getBytes());

            AsyncFileFd readFd = AsyncFileFd.asyncOpen(eventLoop, OwnershipMemory.of(arena.allocateFrom(readFile.getAbsolutePath())), Libc.Fcntl_H.O_RDWR).get();
            AsyncFileFd writeFd = AsyncFileFd.asyncOpen(eventLoop, OwnershipMemory.of(arena.allocateFrom(writeFile.getAbsolutePath())), Libc.Fcntl_H.O_RDWR).get();

            IoUringSyscallOwnershipResult<PipeFd> readResult = splicer.asyncWritePipe(OwnershipResource.wrap(pipeFd), readFd, 0, sampleString.length(), 0).get();
            Assert.assertEquals(sampleString.length(), readResult.result());

            IoUringSyscallOwnershipResult<PipeFd> writeResult = splicer.asyncReadPipe(OwnershipResource.wrap(pipeFd), writeFd, 0, sampleString.length(), 0).get();
            Assert.assertEquals(sampleString.length(), readResult.result());

            byte[] read = new FileInputStream(readFile).readAllBytes();
            Assert.assertEquals(sampleString, new String(read));


            readFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            readFile.deleteOnExit();
            writeFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
            writeFile.deleteOnExit();
            new FileOutputStream(readFile).write(sampleString.getBytes());

            readFd = AsyncFileFd.asyncOpen(eventLoop, OwnershipMemory.of(arena.allocateFrom(readFile.getAbsolutePath())), Libc.Fcntl_H.O_RDWR).get();
            writeFd = AsyncFileFd.asyncOpen(eventLoop, OwnershipMemory.of(arena.allocateFrom(writeFile.getAbsolutePath())), Libc.Fcntl_H.O_RDWR).get();

            var sendFilePipeFd = new OwnershipResourceForTest<>(new PipeFd());
            IoUringSyscallOwnershipResult<PipeFd> sendResult = splicer.asyncSendFile(
                    sendFilePipeFd, readFd, 0, writeFd, 0, sampleString.length(), Libc.Fcntl_H.SPLICE_F_MOVE
            ).get();

            Assert.assertEquals(sampleString.length(), sendResult.result());
            read = new FileInputStream(writeFile).readAllBytes();
            Assert.assertEquals(sampleString, new String(read));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void multiShotAcceptTest() {
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
            eventLoop.start();
            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 5000 + eventLoopType.ordinal());
            AsyncTcpServerSocketFd serverFd = new AsyncTcpServerSocketFd(eventLoop, serverAddress, 5000 + eventLoopType.ordinal());
            serverFd.bind();
            serverFd.listen(16);

            long addrSize = serverFd.addrSize();
            OwnershipMemoryForTest addr = new OwnershipMemoryForTest(Instance.LIBC_MALLOC.malloc(addrSize));
            OwnershipMemoryForTest addrLen = new OwnershipMemoryForTest(Instance.LIBC_MALLOC.malloc(ValueLayout.JAVA_INT.byteSize()));
            ArrayBlockingQueue<Socket> sockets = new ArrayBlockingQueue<>(2);
            IntStream.range(0, 2)
                    .mapToObj(_ -> Thread.ofVirtual().unstarted(() -> {
                        try {
                            Socket socket = new Socket();
                            socket.connect(serverAddress);
                            sockets.offer(socket);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .forEach(Thread::start);
            AsyncMultiShotTcpServerSocketFd serverSocketFd = new AsyncMultiShotTcpServerSocketFd(serverFd);

            AtomicBoolean cqeHasCancel = new AtomicBoolean(false);

            ArrayList<IoUringSyscallResult<AsyncTcpSocketFd>> syscallResults = new ArrayList<>();
            CountDownLatch acceptCondition = new CountDownLatch(1);
            CountDownLatch cancelCondition = new CountDownLatch(1);
            CancelToken cancelToken = serverSocketFd.asyncMultiAccept(0, addr, addrLen, r -> {
                syscallResults.add(r);
                if (syscallResults.size() == 2) {
                    acceptCondition.countDown();
                }
                if (r.canceled()) {
                    cqeHasCancel.set(true);
                    cancelCondition.countDown();
                }
            });
            acceptCondition.await();
            Thread.sleep(1000);
            Map<Integer, Socket> portToSocket = sockets.stream()
                    .collect(Collectors.toMap(Socket::getLocalPort, Function.identity()));
            for (var result : syscallResults) {
                Assert.assertNotNull(result.value());
                Assert.assertEquals(result.value().fd(), result.res());
                var fd = result.value();
                InetSocketAddress remoteAddress = (InetSocketAddress) fd.getRemoteAddress();
                log.info("from async multi mode server socket accept:{}", fd);
                Socket remoteSocket = portToSocket.get(remoteAddress.getPort());
                Assert.assertNotNull(remoteSocket);
            }
            CancelToken.CancelResult cancelResult = cancelToken.cancelOperation(true).get();
            if (cancelResult instanceof CancelToken.SuccessResult r) {
                Assert.assertEquals(1, r.count());
            } else {
                Assert.fail();
            }
            cancelCondition.await();
            Assert.assertTrue(cqeHasCancel.get());
            serverSocketFd.close();
            Thread.sleep(1000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMadvise() throws Exception {
        log.info("start test madvise vt:{}", eventLoopType);
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        try (eventLoop) {
            eventLoop.start();
            File tempFile = File.createTempFile("test4IoUringMmap", ".txt");
            tempFile.deleteOnExit();
            String content = "hello mmap";
            int fileSize = 0;
            try (var output = new FileOutputStream(tempFile)) {
                output.write(content.getBytes());
                fileSize = (int) output.getChannel().size();
            }
            var fd = NativeHelper.useCStr(MemoryAllocator.LIBC_MALLOC, tempFile.getAbsolutePath(), (fileName) -> Instance.LIBC.open(fileName, Libc.Fcntl_H.O_RDWR));
            Assert.assertTrue(fd > 0);

            LibMman libMman = Instance.LIB_MMAN;
            MemorySegment mmapBasePointer = libMman.mmap(MemorySegment.NULL, fileSize, LibMman.Prot.PROT_READ, LibMman.Flag.MAP_SHARED, fd, 0);
            mmapBasePointer = mmapBasePointer.reinterpret(fileSize);

            IoUringSyscallResult<Void> result = IoUringMadvise.asyncMadiveDirect(eventLoop, mmapBasePointer, LibMman.Advice.MADV_SEQUENTIAL | LibMman.Advice.MADV_WILLNEED).get();
            Assert.assertFalse(result.canceled());
            Assert.assertEquals(0, result.res());

            try (
                    FileChannel fileChannel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ);
                    Arena arena = Arena.ofConfined()
            ) {
                MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
                result = IoUringMadvise.asyncMadive(eventLoop, mappedByteBuffer, LibMman.Advice.MADV_SEQUENTIAL | LibMman.Advice.MADV_WILLNEED).get();
                Assert.assertFalse(result.canceled());
                Assert.assertEquals(0, result.res());

                MemorySegment memorySegment = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
                IoUringMadvise.asyncMadive(eventLoop, memorySegment, LibMman.Advice.MADV_SEQUENTIAL | LibMman.Advice.MADV_WILLNEED).get();
                Assert.assertFalse(result.canceled());
                Assert.assertEquals(0, result.res());
            }
        }
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
