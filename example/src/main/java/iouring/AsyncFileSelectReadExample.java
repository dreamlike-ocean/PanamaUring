package iouring;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.uring.IOUringEventLoop;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_RDONLY;

public class AsyncFileSelectReadExample {
    public static void main(String[] args) throws Exception {
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            eventLoop.start();
            AsyncFile readFile = eventLoop.openFile("demo.txt", O_RDONLY());
            byte[] res = readFile.readSelected(0, 1024).get();
            System.out.println(new String(res));
        }
    }
}
