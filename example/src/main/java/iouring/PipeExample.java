package iouring;

import top.dreamlike.async.file.AsyncPipe;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class PipeExample {
    public static void main(String[] args) {
        try (Arena session = Arena.ofConfined();
             IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            eventLoop.start();
            AsyncPipe pipe = new AsyncPipe(eventLoop);
            //offset 没有意义
            byte[] array = "hello world".getBytes();
            MemorySegment allocate = session.allocateArray(JAVA_BYTE, array.length);
            pipe.readUnsafe(2115616, allocate)
                    .thenAccept(i -> {
                        if (i < 0) {
                            System.out.println(NativeHelper.getErrorStr(-i));
                        } else {
                            System.out.println("res:" + new String(allocate.toArray(JAVA_BYTE)));
                        }
                    });
            pipe.write(15, array, 0, array.length)
                    .thenAccept(i -> {
                        if (i < 0) {
                            System.out.println(NativeHelper.getErrorStr(-i));
                        } else {
                            System.out.println("write res:" + i);
                        }
                    }).get();
            Thread.sleep(1000);
        } catch (Exception __) {
            //ignore
        }

    }
}
