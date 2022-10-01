package top.dreamlike;

import top.dreamlike.asyncFile.AsyncFile;
import top.dreamlike.asyncFile.IOOpResult;
import top.dreamlike.asyncFile.IOUring;

import java.lang.foreign.*;
import java.util.List;

import static top.dreamlike.fcntl.fcntl_h.*;

public class main {
    public static void main(String[] args) {
        System.load("/home/dreamlike/uringDemo/src/main/resources/liburing.so");
        MemorySession session = MemorySession.openConfined();
        IOUring uring = new IOUring(4);
        AsyncFile file = uring.openFile("demo.txt", O_RDONLY());
        MemorySegment memorySegment = session.allocateArray(ValueLayout.JAVA_BYTE, 64);
        file.read(0, memorySegment)
                .thenAccept(i -> {
                    var bytes = memorySegment.asSlice(0, i).toArray(ValueLayout.JAVA_BYTE);
                    System.out.println(new String(bytes));
                });


        List<IOOpResult> ioOpResults = uring.waitFd();
        IOOpResult ioOpResult = ioOpResults.get(0);
        ioOpResult.callback.accept(ioOpResult.res);
        session.close();
    }

}
