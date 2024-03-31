package top.dreamlike.panama.uring.trait;

public interface OwnershipResource<T> extends AutoCloseable {
    T resource();

    void drop();

    @Override
    default void close() throws Exception {
        drop();
    }

    static <T> OwnershipResource<T> wrap(T t) {
        return new OwnershipResource<T>() {
            @Override
            public T resource() {
                return t;
            }

            @Override
            public void drop() {

            }
        };
    }
}
