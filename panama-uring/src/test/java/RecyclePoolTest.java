import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.helper.pool.PooledObject;
import top.dreamlike.panama.uring.helper.pool.SimplePool;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

public class RecyclePoolTest {


    @Test
    public void PoolSingleThread() {

        RecyclePool pool = new RecyclePool(2);
        Assert.assertNotNull(pool.acquire());
        Assert.assertNotNull(pool.acquire());
        Assert.assertNull(pool.acquire(1, TimeUnit.SECONDS));
        Assert.assertEquals(2, pool.getSize());
    }

    @Test
    public void PoolMultiThread() throws InterruptedException {
        int poolSize = 10;
        RecyclePool pool = new RecyclePool(poolSize);
        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ArrayBlockingQueue<PooledTmpObject> queue = new ArrayBlockingQueue<>(poolSize);
        CountDownLatch count = new CountDownLatch(threadCount);
        Runnable r = () -> {
            try {
                barrier.await();
                PooledTmpObject object = pool.acquire(1, TimeUnit.SECONDS);
                if (object != null) {
                    queue.offer(object);
                }
                count.countDown();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };

        IntStream.range(0, threadCount).forEach(i -> new Thread(r).start());

        count.await();

        Assert.assertEquals(poolSize, queue.size());
        Assert.assertEquals(poolSize, pool.getSize());
        for (PooledTmpObject object : queue) {
            Assert.assertNotNull(object);
        }
    }




    public static class RecyclePool extends SimplePool<PooledTmpObject> {
        public RecyclePool(int max) {
            super(max);
        }

        @Override
        public PooledTmpObject create() {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(500));
            return new PooledTmpObject();
        }

        public int getSize() {
            return currentSize.get();
        }
    }


    public static class PooledTmpObject implements PooledObject<PooledTmpObject> {
        @Override
        public void destroy() {
        }
    }

}
