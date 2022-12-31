package top.dreamlike;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.uring.EventLoop;

import java.lang.foreign.MemorySession;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_APPEND;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_WRONLY;

public class main {
    public static void main(String[] args) throws Exception {
        EventLoop eventLoop = new EventLoop(16, 4, 1000);
        eventLoop.start();
        AsyncFile asyncFile = eventLoop.openFile("demo.txt", O_APPEND()|O_WRONLY());
        Integer res = asyncFile.write(-1, MemorySession.global().allocateUtf8String("追加的元素"))
                .get();
        System.out.println(res);
        asyncFile.fsync()
                        .thenAccept(i -> System.out.println("fsync:"+i))
                                .get();
       eventLoop.shutdown();

    }


}
