import org.junit.Test;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.nativeLib.eventfd.eventfd_h;


import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static top.dreamlike.nativeLib.eventfd.eventfd_h.eventfd_read;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_APPEND;
import static top.dreamlike.nativeLib.unistd.unistd_h.*;

public class FileTest {



    @Test
    public void writeFile(){
        //RAII
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment filePath = session.allocateUtf8String("demo.txt");
            var fd = open(filePath, O_RDWR() |O_APPEND());
            if (fd == -1){
                System.out.println("open file error!");
                return;
            }
            var str = "hello panama write file".getBytes(StandardCharsets.UTF_8);
            MemorySegment waitWrite = session.allocateArray(ValueLayout.JAVA_BYTE, str.length);
            waitWrite.copyFrom(MemorySegment.ofArray(str));

            System.out.println(write(fd, waitWrite, waitWrite.byteSize()));
            sync();
            close(fd);
        }



    }


    @Test
    public void callGetPid() throws Throwable {
        //int getpid()
        FunctionDescriptor getPidFunctionDesc= FunctionDescriptor.of(JAVA_INT.withBitAlignment(32));
        Linker nativeLinker = Linker.nativeLinker();
        MethodHandle getpid = nativeLinker.downcallHandle(nativeLinker.defaultLookup().lookup("getpid").get(), getPidFunctionDesc);
        int pid = (int)getpid.invokeExact();
        System.out.println(pid);
        new Scanner(System.in).nextLine();
    }


    @Test
    public void epollAndIoUring(){
        MemorySession session = MemorySession.openImplicit();
        Epoll epoll = new Epoll();
        IOUring uring = new IOUring(4);
        uring.registerToEpoll(epoll);
        //异步文件读取
        AsyncFile file = uring.openFile("demo.txt",O_WRONLY()|O_APPEND());
        var str = "modeifa async write \n".getBytes(StandardCharsets.UTF_8);
        MemorySegment waitWrite = session.allocateArray(ValueLayout.JAVA_BYTE, str.length);
        waitWrite.copyFrom(MemorySegment.ofArray(str));
        file.write(0,waitWrite)
                .thenAccept(System.out::println);

        MemorySegment allocate = session.allocate(JAVA_LONG);
        while (true) {
            ArrayList<Epoll.Event> select = epoll.select(-1);
            int eventfd = select.get(0).fd;
            eventfd_read(eventfd,allocate);
            List<IOOpResult> x = uring.batchGetCqe(64);
            System.out.println(x);
            IOOpResult ioOpResult = x.get(0);
            ioOpResult.callback.consumer(ioOpResult.res,ioOpResult.bid);

            x = uring.batchGetCqe(64);
            System.out.println("End "+x);
        }

    }


    //static inline int io_uring_opcode_supported(const struct io_uring_probe *p, int op)
    //{
    //	if (op > p->last_op)
    //		return 0;
    //	return (p->ops[op].flags & IO_URING_OP_SUPPORTED) != 0;
    //}
    @Test
    public void testUringIORING_OP_PROVIDE_BUFFERS() throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(16);

        AsyncFile file = ioUring.openFile("demo.txt",O_RDONLY());

        new Thread(()->{
            while (true){
                ioUring.waitComplete();
                for (IOOpResult ioOpResult : ioUring.batchGetCqe(1024)) {
                    ioOpResult.callback.consumer(ioOpResult.res, ioOpResult.bid);
                }
            }
        }).start();
        ArrayList<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            CompletableFuture<byte[]> read = file.readSelected(0, 1024);
            read.thenAccept(c -> {
                System.out.println(new String(c));
                System.out.println("_________________________________________");
            });
            futures.add(read);
        }

        ioUring.submit();

        for (CompletableFuture<byte[]> future : futures) {
            future.get();
        }


    }

    @Test
    public void eventfd(){
        int eventfd = eventfd_h.eventfd(0, 0);
        MemorySession memorySession = MemorySession.openShared();

        new Thread(()->{
            MemorySegment c1 = memorySession.allocate(eventfd_h.C_LONG);
            c1.set(eventfd_h.C_LONG,0,1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            long read = write(eventfd, c1, eventfd_h.C_LONG.byteSize());
        }).start();
        MemorySegment count = memorySession.allocate(eventfd_h.C_LONG);
        long read = read(eventfd, count, eventfd_h.C_LONG.byteSize());
        System.out.println(count.get(eventfd_h.C_LONG,0));
    }

    @Test
    public void testByteArrayWrite() throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(16,4);

        AsyncFile file = ioUring.openFile("demo.txt",O_RDWR()|O_APPEND());

        new Thread(()->{
            while (true){
                for (IOOpResult ioOpResult : ioUring.waitFd()) {
                    ioOpResult.callback.consumer(ioOpResult.res,ioOpResult.bid);
                }
            }
        }).start();
        var wantWrite = "测试新的api接口".getBytes(StandardCharsets.UTF_8);
        System.out.println(file.write(-1, wantWrite, 0, wantWrite.length).get());

    }

    @Test
    public void testAsyncServer() throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(16,4);
        AsyncServerSocket serverSocket = new AsyncServerSocket(ioUring, "127.0.0.1", 4399);
        new Thread(()->{
            while (true){
                for (IOOpResult ioOpResult : ioUring.waitFd()) {
                    ioOpResult.callback.consumer(ioOpResult.res,ioOpResult.bid);
                }
            }
        }).start();

        AsyncSocket asyncSocket = serverSocket.acceptAsync().get();

        System.out.println(asyncSocket);

        byte[] bytes = asyncSocket.read(1024).get();


        System.out.println("Write:"+asyncSocket.write(bytes, 0, bytes.length).get());

    }

    @Test
    public void testNOOP() throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(16,4);
        CompletableFuture<Void> future = new CompletableFuture<>();
        ioUring.prep_no_op(() -> {
         future.complete(null);
        });
        new Thread(()->{
            while (true){
                ioUring.waitComplete();
                for (IOOpResult ioOpResult : ioUring.batchGetCqe(1024)) {
                    ioOpResult.callback.consumer(ioOpResult.res, ioOpResult.bid);
                }
            }
        }).start();
        ioUring.submit();
        future.thenAccept(v -> System.out.println("no_op end"))
                .get();

        var future2 = new CompletableFuture<>();
        ioUring.prep_no_op(() -> {
            future2.complete(null);
        });
        ioUring.submit();

        future2.thenAccept(v -> System.out.println("no_op end 2"))
                .get();

    }

    @Test
    public void testTimeout() throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(16,4);
        ioUring.waitComplete(-1);
        LocalDateTime start = LocalDateTime.now();
        for (IOOpResult ioOpResult : ioUring.batchGetCqe(16)) {
            System.out.println(ioOpResult.fd);
        }

        ioUring.waitComplete(6_000);

        System.out.println(Duration.between(start,LocalDateTime.now()).getSeconds());
    }
}
