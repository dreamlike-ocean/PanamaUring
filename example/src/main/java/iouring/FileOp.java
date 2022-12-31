package iouring;

import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.uring.IOUring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;

public class FileOp {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        IOUring ioUring = new IOUring(11);
        AsyncFile appendFile = ioUring.openFile("demo.txt", O_APPEND() | O_WRONLY());
        startPoller(ioUring);
        byte[] bytes = "追加写东西".getBytes();
        CompletableFuture<Integer> write = appendFile.write(-1, bytes, 0, bytes.length);
        //要提交
        ioUring.submit();
        Integer integer = write.get();
        System.out.println("async read res:"+integer);
        AsyncFile readFile = ioUring.openFile("demo.txt", O_RDONLY());
        try (MemorySession memorySession = MemorySession.openConfined()) {
            MemorySegment memorySegment = memorySession.allocate(1024);
            CompletableFuture<Integer> read = readFile.read(0, memorySegment);
            //要提交
            ioUring.submit();
            Integer length = read.get();
            byte[] res = memorySegment.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
            System.out.println("read all :"+new String(res));
        }
    }


    public static void startPoller(IOUring uring){
        Thread thread = new Thread(() -> {
            while (true) {
                //等待至少一个任务
                uring.waitComplete(-1);
                uring.batchGetCqe(16).forEach(IOOpResult::doCallBack);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
