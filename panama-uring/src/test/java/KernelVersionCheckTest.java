import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

import java.util.concurrent.ArrayBlockingQueue;

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
        Assert.assertEquals(1, queue.size());
        Throwable throwable = queue.take();
        Assert.assertTrue(throwable instanceof UnsupportedOperationException);
    }


}
