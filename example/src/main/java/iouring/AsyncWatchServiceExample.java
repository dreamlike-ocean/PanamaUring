package iouring;

import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.FileEvent;
import top.dreamlike.nativeLib.inotify.inotify_h;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AsyncWatchServiceExample {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        IOUringEventLoop ioUringEventLoop = new IOUringEventLoop(32, 16, 100);
        ioUringEventLoop.start();
        var watchService = new AsyncWatchService(ioUringEventLoop);
        int demoFd = watchService.register(Path.of("demo.txt"), inotify_h.IN_MODIFY()).get();
        System.out.println(demoFd);
        int dirFd = watchService.register(Path.of("/home/dreamlike/uringDemo"), inotify_h.IN_CREATE()).get();
        System.out.println(dirFd);
        System.out.println("select");
        while (true) {
            List<FileEvent> fileEvents = watchService.select().get();
            for (FileEvent fileEvent : fileEvents) {
                System.out.println(fileEvent);
            }
        }
    }
}
