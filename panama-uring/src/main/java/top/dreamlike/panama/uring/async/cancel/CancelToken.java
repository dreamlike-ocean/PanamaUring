package top.dreamlike.panama.uring.async.cancel;

import java.util.concurrent.CompletableFuture;

public interface CancelToken {

        /**
        * Cancel the operation.
        */
        CompletableFuture<Integer> cancel();

        /**
        * Check if the operation has been cancelled.
        *
        * @return {@code true} if the operation has been cancelled.
        */
        boolean isCancelled();

}
