package top.dreamlike.extension.fp;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public sealed interface Result<T, E extends Throwable> permits Result.Err, Result.OK {
    static <T> Consumer<Result<T, Throwable>> transform(CompletableFuture<T> future) {
        return (r -> {
            switch (r) {
                case OK<T>(T t) -> future.complete(t);
                case Err<T, Throwable>(Throwable throwable) -> future.completeExceptionally(throwable);
            }
        });
    }

    default T unwrap() {
        if (this instanceof Result.OK) {
            return ((OK<T>) this).t;
        }
        throw new IllegalStateException("Result is Err");
    }

    public record OK<T>(T t) implements Result<T, Throwable> {
    }

    public record Err<T, E extends Throwable>(E e) implements Result<T, E> {
    }
}
