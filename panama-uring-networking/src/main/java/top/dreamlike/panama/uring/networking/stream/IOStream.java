package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.trait.IoUringSocketOperator;
import top.dreamlike.panama.uring.networking.stream.pipeline.IOStreamPipeline;

import java.util.concurrent.CompletableFuture;

public abstract sealed class IOStream<T extends IoUringSocketOperator> permits MultiShotSocketStream,SocketStream {


    public abstract void close();

    public abstract IOStreamPipeline<T> pipeline();

    public abstract void setAutoRead(boolean autoRead);

    public void startWrite(Object msg, CompletableFuture<Void> promise) {
        pipeline().fireWrite(msg, promise);
    }

    public abstract T socket();



}
