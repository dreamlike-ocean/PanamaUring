package iouring;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.eventloop.IOUringEventLoop;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;

public class FileOpExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        IOUringEventLoop ioUringEventLoop = new IOUringEventLoop(32, 16, 100);
        AsyncFile appendFile = ioUringEventLoop.openFile("demo.txt", O_APPEND() | O_WRONLY() | O_CREAT());

        ioUringEventLoop.start();

        byte[] bytes = "追加写东西".getBytes();
        CompletableFuture<Integer> write = appendFile.write(-1, bytes, 0, bytes.length);


        Integer integer = write.get();
        System.out.println("async write res:"+integer);
        AsyncFile readFile = ioUringEventLoop.openFile("demo.txt", O_RDONLY());
        try (MemorySession memorySession = MemorySession.openShared()) {
            MemorySegment memorySegment = memorySession.allocate(1024);
            CompletableFuture<Integer> read = readFile.readUnsafe(0, memorySegment);
            Integer length = read.get();
            byte[] res = memorySegment.asSlice(0, length).toArray(ValueLayout.JAVA_BYTE);
            System.out.println("read all :"+new String(res));
        }

        ioUringEventLoop.shutdown();


    }


}
