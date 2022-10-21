import org.junit.Test;
import top.dreamlike.asyncFile.AsyncFile;
import top.dreamlike.asyncFile.IOOpResult;
import top.dreamlike.asyncFile.IOUring;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.nativeLib.liburing.io_uring;
import top.dreamlike.nativeLib.liburing.io_uring_probe;


import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

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
        System.load("/home/dreamlike/uringDemo/src/main/resources/liburing.so");
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
            List<IOOpResult> x = uring.peekCqe(64);
            System.out.println(x);
            IOOpResult ioOpResult = x.get(0);
            ioOpResult.callback.consumer(ioOpResult.res,ioOpResult.bid);

            x = uring.peekCqe(64);
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
        System.load("/home/dreamlike/uringDemo/src/main/resources/liburing.so");
        IOUring ioUring = new IOUring(16);

        AsyncFile file = ioUring.openFile("demo.txt",O_RDONLY());

        new Thread(()->{
            while (true){
                for (IOOpResult ioOpResult : ioUring.waitFd()) {
                    ioOpResult.callback.consumer(ioOpResult.res,ioOpResult.bid);
                }
            }
        }).start();
        for (int i = 0; i < 6; i++) {
            CompletableFuture<byte[]> read = file.read(0, 1024);
            read.thenAccept(c -> {
                System.out.println(new String(c));
                System.out.println("_________________________________________");
            });
            read.get();
        }


    }

}
