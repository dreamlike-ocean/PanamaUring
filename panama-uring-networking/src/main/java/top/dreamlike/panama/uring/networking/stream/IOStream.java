package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.networking.stream.pipeline.IOStreamPipeline;

import java.util.concurrent.CompletableFuture;

public abstract sealed class IOStream<T extends IoUringOperator> permits MultiShotSocketStream, FileStream {



    public abstract void close();

    public abstract IOStreamPipeline pipeline();

    public abstract void setAutoRead(boolean autoRead);

    public abstract void startWrite(Object msg, CompletableFuture<Integer> promise);

    public abstract T fd();
}
