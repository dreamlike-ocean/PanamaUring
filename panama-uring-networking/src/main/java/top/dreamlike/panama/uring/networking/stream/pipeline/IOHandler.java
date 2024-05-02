package top.dreamlike.panama.uring.networking.stream.pipeline;

import java.util.concurrent.Executor;

public interface IOHandler {

    default Executor executor() {
        return null;
    }

    default void onHandleAdded(IOStreamPipeline.IOContext context) {

    }

    default void onHandleRemoved(IOStreamPipeline.IOContext context) {

    }

    default void onHandleActive(IOStreamPipeline.IOContext context) {

    }

    default void onHandleInactive(IOStreamPipeline.IOContext context) {

    }

    void onRead(IOStreamPipeline.IOContext context, Object msg);

    default void onWrite(IOStreamPipeline.IOContext context, Object msg) {
        context.fireNextWrite(msg);
    }
}
