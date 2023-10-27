package top.dreamlike.helper;

public interface ThrowableFunction<T, R, E extends Throwable> {

    R apply(T t) throws E;

}
