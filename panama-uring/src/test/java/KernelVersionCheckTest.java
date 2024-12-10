import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class KernelVersionCheckTest {

    private static final Logger log = LoggerFactory.getLogger(KernelVersionCheckTest.class);

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
                log.info("will setOpCode");
                sqe.setOpcode(Byte.MAX_VALUE);
            });
            Thread.sleep(1_000);
        } catch (Throwable r) {
            throw new RuntimeException(r);
        }
        eventLoop.join();
        Throwable throwable = queue.poll(10, TimeUnit.SECONDS);
        Assert.assertTrue(throwable instanceof UnsupportedOperationException);
    }


}
