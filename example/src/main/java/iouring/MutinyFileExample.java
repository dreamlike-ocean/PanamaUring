package iouring;

import io.smallrye.mutiny.subscription.Cancellable;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.memory.DefaultOwnershipMemory;
import top.dreamlike.extension.memory.OwnershipMemory;
import top.dreamlike.helper.Pair;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;

public class MutinyFileExample {
    public static void main(String[] args) {

        try (IOUringEventLoop ioUringEventLoop = new IOUringEventLoop(32, 16, 100);
             Arena session = Arena.ofConfined()) {

            AsyncFile appendFile = ioUringEventLoop.openFile("demo.txt", O_APPEND() | O_WRONLY() | O_CREAT());

            ioUringEventLoop.start();

            byte[] bytes = "追加写东西Lazy".getBytes();
            MemorySegment memorySegment = session.allocate(bytes.length);
            MemorySegment.copy(bytes, 0, memorySegment, JAVA_BYTE, 0, bytes.length);
            Pair<OwnershipMemory, Integer> res = appendFile.writeUnsafeLazy(-1, new DefaultOwnershipMemory(memorySegment, m -> {
                System.out.println("dont release");
            })).await().indefinitely();
            System.out.println(res.t2());

            Cancellable cancelable = appendFile.writeUnsafeLazy(-1, new DefaultOwnershipMemory(memorySegment, m -> {
                System.out.println("Im released");
            })).subscribe().with(p -> System.out.println("dont print"));

            cancelable.cancel();
            //小睡一会 避免先shutdown了eventloop
            Thread.sleep(1000);

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
