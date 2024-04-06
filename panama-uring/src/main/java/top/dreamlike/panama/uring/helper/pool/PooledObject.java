package top.dreamlike.panama.uring.helper.pool;

public interface PooledObject<T extends PooledObject<T>> {
    void destroy();
}
