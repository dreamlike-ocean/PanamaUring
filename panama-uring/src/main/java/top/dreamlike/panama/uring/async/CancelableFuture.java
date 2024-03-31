package top.dreamlike.panama.uring.async;

import top.dreamlike.panama.uring.async.cancel.CancelToken;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CancelableFuture<T> extends CompletableFuture<T> implements CancelToken {
    private CancelToken token;

    public CancelableFuture(CancelToken token) {
        this.token = token;
    }

    public CancelableFuture(Function<CancelableFuture<T>, CancelToken> factory) {
        this.token = factory.apply(this);
    }

    @Override
    public CompletableFuture<Integer> cancel() {
        return token.cancel();
    }

    @Override
    public boolean isCancelled() {
        return token.isCancelled();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean res = super.cancel(mayInterruptIfRunning);
        this.cancel();
        return res;
    }

    @Override
    public <U> CancelableFuture<U> newIncompleteFuture() {
        return new CancelableFuture<>(token);
    }
}
