package top.dreamlike.panama.uring.async.cancel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CancelableFuture<T> extends CompletableFuture<T> implements CancelToken {
    protected CancelToken token;

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
    public CompletableFuture<Integer> ioUringCancel(boolean needSubmit) {
        return token.ioUringCancel(needSubmit);
    }


    @Override
    public boolean isCancelled() {
        return token.isCancelled();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
       throw new UnsupportedOperationException("CancelableFuture does not support this operation, please call CancelableFuture::cancel()");
    }

    @Override
    public <U> CancelableFuture<U> newIncompleteFuture() {
        return new CancelableFuture<>(token);
    }

    public CancelToken token() {
        return token;
    }
}
