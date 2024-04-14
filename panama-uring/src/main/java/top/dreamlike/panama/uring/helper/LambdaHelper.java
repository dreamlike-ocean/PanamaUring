package top.dreamlike.panama.uring.helper;


import java.util.concurrent.Callable;

public class LambdaHelper {
    public static class LazyValueHolder<T> {
        public T value;
    }

    public static <T> T runWithThrowable(Callable<T> c) {
        try {
            return c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}