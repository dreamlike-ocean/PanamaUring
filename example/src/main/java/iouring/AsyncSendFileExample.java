package iouring;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.StackValue;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_RDONLY;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_WRONLY;

public class AsyncSendFileExample {
    public static void main(String[] args) throws Exception {
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100);
             StackValue stackValue = StackValue.currentStack()) {
            eventLoop.start();
            AsyncFile file = eventLoop.openFile("target/PanamaUring-1.0.pom", O_RDONLY());
            AsyncFile targetFile = eventLoop.openFile("demo.txt", O_WRONLY());
            long size = file.size();
            eventLoop.sendFile(file, targetFile, 0, size)
                    .await().indefinitely();
        }
    }
}
