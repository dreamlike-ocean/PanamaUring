package top.dreamlike.panama.uring.networking.stream.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public interface IOHandler {

    default Executor executor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    default void onHandleAdded(IOStreamPipeline.IOContext context) {

    }

    default void onHandleRemoved(IOStreamPipeline.IOContext context) {

    }

    default void onHandleInactive(IOStreamPipeline.IOContext context) {

    }

    void onRead(IOStreamPipeline.IOContext context, Object msg);

    default void onError(IOStreamPipeline.IOContext context, Throwable cause) {

    }

    default void onWrite(IOStreamPipeline.IOContext context, Object msg, CompletableFuture<Void> promise) {
        context.fireNextWrite(msg, promise);
    }
}
