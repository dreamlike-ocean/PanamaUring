import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class KernelVersionCheckTest {

    @Test
    public void test() throws InterruptedException {
        IoUringEventLoop eventLoop = new IoUringEventLoop(params -> {
            params.setSq_entries(4);
            params.setFlags(0);
        });
        ArrayBlockingQueue<Throwable> queue = new ArrayBlockingQueue<>(1);
        eventLoop.setExceptionHandler(queue::add);
        eventLoop.start();
        try(eventLoop) {
            eventLoop.asyncOperation(sqe -> {
                sqe.setOpcode(Byte.MAX_VALUE);
            });
        } catch (Throwable r) {
            throw new RuntimeException(r);
        }
        eventLoop.join();
        Throwable throwable = queue.poll(10, TimeUnit.SECONDS);
        Assert.assertTrue(throwable instanceof UnsupportedOperationException);
    }


}
