package top.dreamlike.panama.uring.helper.pool;

import java.util.concurrent.TimeUnit;

public interface Pool<T extends PooledObject<T>> {

    T acquire();

    T acquire(long timeout, TimeUnit timeUnit);

    void release(T t);
}
