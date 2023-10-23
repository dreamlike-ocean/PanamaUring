package iouring;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.file.AsyncPipe;
import top.dreamlike.eventloop.IOUringEventLoop;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_RDONLY;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_WRONLY;

public class AsyncSpliceExample {
    public static void main(String[] args) throws Exception {
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            eventLoop.start();
            AsyncFile file = eventLoop.openFile("README.md", O_RDONLY());
            AsyncFile targetFile = eventLoop.openFile("demo.txt", O_WRONLY());
            AsyncPipe pipe = new AsyncPipe(eventLoop);
            System.out.println(file);
            System.out.println(pipe);
            CompletableFuture<Void> promise = new CompletableFuture<>();
            eventLoop.submitLinkedOpSafe((state) -> {
                eventLoop.spliceLazy(file, pipe, MemorySegment.NULL, 1024)
                        .subscribe().with(i -> System.out.println(STR. "file -> pipe res: \{ i }" ));
                state.turnoff();
                eventLoop.spliceLazy(pipe, targetFile, MemorySegment.NULL, 1024)
                        .subscribe().with(i -> {
                            System.out.println(STR. "pipe -> file res: \{ i }" );
                            promise.complete(null);
                        });
            });

            promise.get();
        }
    }
}
