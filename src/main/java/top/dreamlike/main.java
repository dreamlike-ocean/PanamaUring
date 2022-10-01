package top.dreamlike;

import top.dreamlike.asyncFile.AsyncFile;
import top.dreamlike.asyncFile.IOOpResult;
import top.dreamlike.asyncFile.IOUring;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static top.dreamlike.fcntl.fcntl_h.*;
import static top.dreamlike.unistd.unistd_h.*;

public class main {
    public static void main(String[] args) {
        System.load("/home/dreamlike/uringDemo/src/main/resources/liburing.so");
        MemorySession session = MemorySession.openConfined();
        IOUring uring = new IOUring(4);
        AsyncFile file = uring.openFile("demo.txt",O_WRONLY()|O_APPEND());
        var str = "async write \n".getBytes(StandardCharsets.UTF_8);
        MemorySegment waitWrite = session.allocateArray(ValueLayout.JAVA_BYTE, str.length);
        waitWrite.copyFrom(MemorySegment.ofArray(str));

        file.write(0,waitWrite)
                .thenAccept(System.out::println);
        List<IOOpResult> ioOpResults = uring.waitFd();
        IOOpResult ioOpResult = ioOpResults.get(0);
        ioOpResult.callback.accept(ioOpResult.res);
        session.close();
    }

}
