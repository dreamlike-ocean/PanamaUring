package top.dreamlike.panama.uring.async.cancel;

import top.dreamlike.panama.uring.nativelib.libs.Libc;

import java.util.concurrent.CompletableFuture;

public interface CancelToken {

    /**
     * Cancel the operation.
     */
    CompletableFuture<Integer> cancel();

    default CompletableFuture<CancelResult> cancelOperation() {
        return cancel()
                .thenApply(i -> {
                    if (i >= 0) {
                        return new SuccessResult(i);
                    }
                    if (i == -Libc.Error_H.ENOENT) {
                        return new NoElementResult();
                    } else if (i == -Libc.Error_H.EINVAL) {
                        return new InvalidResult();
                    } else if (i == -Libc.Error_H.EALREADY) {
                        return new AlreadyResult();
                    } else {
                        return new OtherError(i);
                    }
                });
    }

    /**
     * Check if the operation has been cancelled.
     *
     * @return {@code true} if the operation has been cancelled.
     */
    boolean isCancelled();


    public sealed interface CancelResult permits AlreadyResult, InvalidResult, NoElementResult, OtherError, SuccessResult {
    }

    public final class NoElementResult implements CancelResult {
    }

    public final class InvalidResult implements CancelResult {
    }

    public final class AlreadyResult implements CancelResult {
    }

    public final record SuccessResult(int count) implements CancelResult {
    }

    public final record OtherError(int errno) implements CancelResult {
    }
}
