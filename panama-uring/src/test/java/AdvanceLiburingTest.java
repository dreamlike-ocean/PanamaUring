import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.fd.AsyncFileFd;
import top.dreamlike.panama.uring.async.fd.AsyncInotifyFd;
import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpSocketFd;
import top.dreamlike.panama.uring.async.fd.AsyncTcpSocketFd;
import top.dreamlike.panama.uring.async.operator.IoUringNoOp;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.helper.Pair;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.helper.OSIoUringProbe;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufRingSetupResult;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringCqe;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RunWith(Parameterized.class)
public class AdvanceLiburingTest {

    private final static Logger log = LoggerFactory.getLogger(AdvanceLiburingTest.class);
    
    public final IoUringEventLoopGetter.EventLoopType eventLoopType;

    public AdvanceLiburingTest(IoUringEventLoopGetter.EventLoopType eventLoopType) {
        this.eventLoopType = eventLoopType;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return IoUringEventLoopGetter.EventLoopType.values();
    }

    @Test
    public void testSelectedRead() {
        log.info("start testSelectedRead");
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
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

            String actual = NativeHelper.bufToString(memory.resource(), (int) memory.resource().byteSize());
            Assert.assertEquals(hello, actual);

            IoUringBufferRingElement ringElement = (IoUringBufferRingElement) memory;
            int bid = ringElement.bid();
            Assert.assertEquals(bufferRing, ringElement.ring());
            Assert.assertTrue(bufferRing.getMemoryByBid(bid).hasOccupy());
            memory.drop();
            Assert.assertFalse(bufferRing.getMemoryByBid(bid).hasOccupy());

            memory = asyncFileFd.asyncSelectedRead(1024, 0).get();

            memory = asyncFileFd.asyncSelectedReadResult(1024, 0).get().value();
            actual = NativeHelper.bufToString(memory.resource(), (int) memory.resource().byteSize());
            Assert.assertEquals(hello, actual);
            Assert.assertFalse(bufferRing.hasAvailableElements());

            ExecutionException exception = Assert.assertThrows(ExecutionException.class, () -> {
                asyncFileFd.asyncSelectedRead(1024, 0).get();
            });
            SyscallException syscallException = (SyscallException) exception.getCause();
            Assert.assertEquals(Libc.Error_H.ENOBUFS, syscallException.getErrorno());
            bufferRing.releaseRing();
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    @Test
    public void testSelectedRecv() {
        log.info("start testSelectedRecv");
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        try (eventLoop) {
            eventLoop.start();
            String hello = "hello new selected recv";
            IoUringBufRingSetupResult ringSetupResult = eventLoop.setupBufferRing(2, 1024, (short) 1).get();
            IoUringBufferRing bufferRing = ringSetupResult.bufRing();
            Assert.assertNotNull(bufferRing);
            Assert.assertFalse(ringSetupResult.res() < 0);

            Path udsPath = Path.of(NativeHelper.JAVA_IO_TMPDIR).resolve(UUID.randomUUID() + ".sock");
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(udsPath);
            ArrayBlockingQueue<SocketChannel> oneshot = new ArrayBlockingQueue<>(1);
            CountDownLatch listenCondition = new CountDownLatch(1);
            Thread.startVirtualThread(() -> {
                try {
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
                    serverSocketChannel.bind(address);
                    listenCondition.countDown();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    oneshot.offer(socketChannel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            listenCondition.await();
            AsyncTcpSocketFd socketFd = new AsyncTcpSocketFd(eventLoop, address);
            Integer res = socketFd.asyncConnect().get();
            Assert.assertTrue(res >= 0);
            byte[] helloBytes = hello.getBytes();
            socketFd.bindBufferRing(bufferRing);
            //server send buffer
            SocketChannel socketChannel = oneshot.take();
            ByteBuffer buffer = ByteBuffer.allocate(helloBytes.length);
            buffer.put(helloBytes);
            buffer.flip();
            socketChannel.write(buffer);

            OwnershipMemory ownershipMemory = socketFd.asyncRecvSelected(helloBytes.length, 0).get();
            Assert.assertEquals(helloBytes.length, ownershipMemory.resource().byteSize());
            Assert.assertEquals(hello, NativeHelper.bufToString(ownershipMemory.resource(), helloBytes.length));

            IoUringBufferRingElement ringElement = PanamaUringSecret.lookupOwnershipBufferRingElement.apply(ownershipMemory);
            int bid = ringElement.bid();
            Assert.assertEquals(bufferRing, ringElement.ring());
            Assert.assertTrue(bufferRing.getMemoryByBid(bid).hasOccupy());
            ownershipMemory.drop();
            Assert.assertFalse(bufferRing.getMemoryByBid(bid).hasOccupy());

            bufferRing.releaseRing();
            Files.delete(udsPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWatchService() {
        log.info("start testWatchService");
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        try (eventLoop;
             OwnershipMemory readBuffer = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(1024)) {
            eventLoop.start();
            AsyncInotifyFd fd = new AsyncInotifyFd(eventLoop);
            Path path = Files.createTempDirectory("test");
            AsyncInotifyFd.WatchKey watchKey = fd.register(path, Libc.Inotify_H.IN_CREATE | Libc.Inotify_H.IN_DELETE);

            CancelableFuture<Pair<OwnershipMemory, List<AsyncInotifyFd.InotifyEvent>>> pollRes = fd.asyncPoll(readBuffer);

            Path resolve = path.resolve("test.txt");
            Files.createFile(resolve);

            Pair<OwnershipMemory, List<AsyncInotifyFd.InotifyEvent>> pair = pollRes.get();
            List<AsyncInotifyFd.InotifyEvent> events = pair.t2();
            Assert.assertEquals(1, events.size());
            AsyncInotifyFd.InotifyEvent event = events.get(0);
            Assert.assertEquals(Libc.Inotify_H.IN_CREATE, event.mask());
            Assert.assertEquals(resolve.toFile().getName(), event.name());

            Files.delete(resolve);
            pollRes = fd.asyncPoll(readBuffer);
            pair = pollRes.get();
            events = pair.t2();
            Assert.assertEquals(1, events.size());
            event = events.get(0);
            Assert.assertEquals(Libc.Inotify_H.IN_DELETE, event.mask());
            Assert.assertEquals(resolve.toFile().getName(), event.name());

            Files.delete(path);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testMultiRecv() throws Exception {
        log.info("start testMultiRecv");
        Path udsPath = Path.of(NativeHelper.JAVA_IO_TMPDIR).resolve(UUID.randomUUID() + ".sock");
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(udsPath);
        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(address);
        ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
        Future<SocketChannel> getTestSocket = threadPool.submit(serverChannel::accept);

        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });

        try (eventLoop) {
            eventLoop.start();
            AsyncTcpSocketFd socketFd = new AsyncTcpSocketFd(eventLoop, address);
            Integer connectRes = socketFd.asyncConnect().get();
            Assert.assertTrue(connectRes >= 0);

            IoUringBufRingSetupResult result = eventLoop.setupBufferRing(4, 1024, (short) 1, 4).get();
            Assert.assertNotNull(result.bufRing());
            Assert.assertTrue(result.res() >= 0);

            socketFd.bindBufferRing(result.bufRing());

            AsyncMultiShotTcpSocketFd targetFd = new AsyncMultiShotTcpSocketFd(socketFd);

            SocketChannel writeSide = getTestSocket.get();

            String hello1 = "hello multi recv1";
            String hello2 = "hello multi recv2";
            writeSide.write(ByteBuffer.wrap(hello1.getBytes()));
            ArrayBlockingQueue<Pair<String, OwnershipMemory>> strings = new ArrayBlockingQueue<>(2);

            AtomicBoolean cancel = new AtomicBoolean(false);
            Set<String> checkMsgSet = Set.of(hello1, hello2);
            CountDownLatch cancelCondition = new CountDownLatch(1);
            var token = targetFd.asyncRecvMulti(event -> {
                if (cancel.get()) {
                    cancelCondition.countDown();
                    return;
                }
                Assert.assertTrue(event.res() >= 0);
                OwnershipMemory memory = event.value();
                String hello = NativeHelper.bufToString(memory.resource(), (int) memory.resource().byteSize());

                memory.drop();
                strings.offer(new Pair<>(hello, memory));
            });
            Consumer<Pair<String, OwnershipMemory>> checker = (p) -> {
                String msg = p.t1();
                OwnershipMemory element = p.t2();
                Assert.assertTrue(checkMsgSet.contains(msg));
                IoUringBufferRingElement ringElement = (IoUringBufferRingElement) element;
                Assert.assertTrue(ringElement.hasOccupy());
                element.drop();
                Assert.assertFalse(ringElement.ring().getMemoryByBid(ringElement.bid()).hasOccupy());
            };
            checker.accept(strings.take());
            writeSide.write(ByteBuffer.wrap(hello2.getBytes()));
            checker.accept(strings.take());

            cancel.set(true);
            token.ioUringCancel(true);
            cancelCondition.await();

        }

    }

    @Test
    public void testLinked() throws Exception {
        log.info("start testLinked");
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        try (eventLoop) {
            IoUringNoOp ioUringNoOp = new IoUringNoOp(eventLoop);
            eventLoop.start();
            eventLoop.linkedScope(() -> {
                var tmp = ioUringNoOp;
                AtomicReference<IoUringSqe> t = new AtomicReference<>();
                eventLoop.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_nop(sqe);
                    t.set(sqe);
                }).thenAccept(cqe -> queue.add(cqe.getRes()));
                Assert.assertTrue(t.get().isLinked());

                eventLoop.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_nop(sqe);
                    t.set(sqe);
                }).thenAccept(cqe -> queue.add(cqe.getRes()));
                Assert.assertTrue(t.get().isLinked());
            }, () -> {
                AtomicReference<IoUringSqe> t = new AtomicReference<>();
                eventLoop.asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_nop(sqe);
                    t.set(sqe);
                }).thenAccept(cqe -> queue.add(cqe.getRes()));
                Assert.assertFalse(t.get().isLinked());
            });
            Thread.sleep(500);
        }
        eventLoop.join();
        Assert.assertEquals(3, queue.size());
        for (Integer i : queue) {
            Assert.assertEquals(Integer.valueOf(0), i);
        }
    }

    @Test
    public void testSendMessageIoUring() throws ExecutionException, InterruptedException {
        log.info("start testSendMessageIoUring for self");
        Consumer<IoUringParams> paramsFactory = params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        };
        IoUringEventLoop eventLoop = IoUringEventLoopGetter.get(eventLoopType, paramsFactory);
        eventLoop.start();
        int message = 20010329;
        AtomicInteger target = new AtomicInteger(0);
        CountDownLatch count = new CountDownLatch(1);
        IoUringCqe ioUringCqe = eventLoop.sendMessage(eventLoop, message, (cqe) -> {
            target.set(cqe.getRes());
            count.countDown();
        }).get();
        Assert.assertEquals(0, ioUringCqe.getRes());
        count.await();
        Assert.assertEquals(message, target.get());

        log.info("start testSendMessageIoUring for other");
        IoUringEventLoop peerEventLoop = IoUringEventLoopGetter.get(eventLoopType, paramsFactory);
        peerEventLoop.start();
        Thread peerThread = peerEventLoop.runOnEventLoop(Thread::currentThread).get();

        message = 20010214;
        CompletableFuture<Pair<Integer, Thread>> completableFuture = new CompletableFuture<>();
        ioUringCqe = eventLoop.sendMessage(peerEventLoop, message, (cqe) -> {
            completableFuture.complete(new Pair<>(cqe.getRes(), peerThread));
        }).get();

        Assert.assertEquals(0, ioUringCqe.getRes());
        Pair<Integer, Thread> pair = completableFuture.join();
        Assert.assertEquals(Integer.valueOf(message), pair.t1());
        Assert.assertEquals(peerThread, pair.t2());

        log.info("start testSendMessageIoUring for rawFd");

       Assert.assertThrows(IllegalArgumentException.class,() -> {
           eventLoop.sendMessage(PanamaUringSecret.findUring.apply(eventLoop).getRing_fd(),1, 1);
       });

        MemorySegment ioUringMemory = Instance.LIBC_MALLOC.malloc(IoUring.LAYOUT.byteSize());
        IoUring internalRing = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringMemory);
        MemorySegment ioUringParamMemory = Instance.LIBC_MALLOC.malloc(IoUringParams.LAYOUT.byteSize());
        IoUringParams ioUringParams = Instance.STRUCT_PROXY_GENERATOR.enhance(ioUringParamMemory);
        paramsFactory.accept(ioUringParams);
        int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
        if (initRes < 0) {
            Instance.LIBC_MALLOC.free(ioUringMemory);
            throw new SyscallException(initRes);
        }

        ioUringCqe = eventLoop.sendMessage(internalRing.getRing_fd(), message, message).join();
        Assert.assertEquals(0, ioUringCqe.getRes());
        Instance.LIB_URING.io_uring_submit_and_wait(internalRing, 1);

        var cqeArrayPtr = Instance.LIBC_MALLOC.malloc(ValueLayout.ADDRESS.byteSize() * 4);
        int cqe_count =  Instance.LIB_URING.io_uring_peek_batch_cqe(internalRing, cqeArrayPtr, 4);
        Assert.assertEquals(1, cqe_count);

        IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(cqeArrayPtr.getAtIndex(ValueLayout.ADDRESS, 0));
        Assert.assertEquals(message, nativeCqe.getRes());
        Assert.assertEquals(message, nativeCqe.getUser_data());
    }

    @Test
    public void testProbe() {
        OSIoUringProbe probe = new OSIoUringProbe();
        int lastOp = probe.getLastOp();
        Assert.assertTrue(lastOp > 0);
        Assert.assertEquals(lastOp + 1, probe.getOps().length);
    }
}
