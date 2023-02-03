package iouring;

import top.dreamlike.async.file.AsyncEventFd;
import top.dreamlike.async.uring.IOUringEventLoop;

import static top.dreamlike.nativeLib.eventfd.eventfd_h.EFD_SEMAPHORE;

public class AsyncEventFdExample {


    public static void main(String[] args) throws Exception {
        try (IOUringEventLoop eventLoop = new IOUringEventLoop(32, 8, 100)) {
            eventLoop.start();

            AsyncEventFd eventFd = new AsyncEventFd(eventLoop, EFD_SEMAPHORE());

            eventFd.read()
                    .whenComplete((c, t) -> {
                        if (t != null) {
                            t.printStackTrace();
                            return;
                        }
                        System.out.println("count:" + c);
                    });

            Integer res = eventFd.write(10).get();
            System.out.println("write res:" + res);
            Thread.sleep(1000);

        }

    }
}
