package top.dreamlike.panama.uring.helper.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SimplePool<T extends PooledObject<T>> implements Pool<T> {

    protected final int max;

    protected final AtomicInteger currentSize;

    protected final ArrayBlockingQueue<T> pooledObjects;

    public SimplePool(int max) {
        this.max = max;
        this.pooledObjects = new ArrayBlockingQueue<>(max);
        this.currentSize = new AtomicInteger(0);
    }

    @Override
    public void release(T t) {
        if (pooledObjects.offer(t)) {
            throw new IllegalStateException("pool has been full");
        }
    }


    protected abstract T create();

    @Override
    public T acquire() {
        return acquire(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Override
    public T acquire(long timeout, TimeUnit timeUnit) {
        //fast path
        T t = pooledObjects.poll();
        if (t != null) {
            return t;
        }
        while (true) {
            int current = currentSize.get();
            if (current == max) {
                try {
                    t = pooledObjects.poll(timeout, timeUnit);
                    return t;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            //乐观新增数据 这样允许多个线程同时新增
            try {
                T mayReturn = create();
                while (true) {
                    int nowSize = currentSize.get();
                    if (nowSize + 1 <= max) {
                        if (currentSize.compareAndSet(nowSize, nowSize + 1)) {
                            return mayReturn;
                        }
                    } else {
                        //本次新增失败了 释放资源 然后上去等poll
                        mayReturn.destroy();
                        break;
                    }
                }

            } catch (Throwable _) {
                continue;
            }
        }
    }
}
