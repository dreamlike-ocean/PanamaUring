package top.dreamlike.async.file;

import top.dreamlike.async.PlainAsyncFd;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.Uni;

import static java.lang.foreign.ValueLayout.JAVA_LONG;

public class AsyncEventFd extends PlainAsyncFd {

    private final int eventfd;


    public AsyncEventFd(IOUringEventLoop eventLoop) {
        this(eventLoop, 0);
    }

    public AsyncEventFd(IOUringEventLoop eventLoop, int flag) {
        super(eventLoop);
        eventfd = NativeHelper.createEventFd(flag);
    }

    public CompletableFuture<Integer> write(long count) {
        Arena shared = Arena.openShared();
        MemorySegment writeBuf = shared.allocate(JAVA_LONG, count);
        return writeUnsafe(0, writeBuf)
                .whenComplete((__, ___) -> {
                    shared.close();
                });
    }

    

    public CompletableFuture<Long> read() {
        Arena shared = Arena.openShared();
        MemorySegment writeBuf = shared.allocate(JAVA_LONG, 0);
        return readUnsafe(0, writeBuf)
                .thenCompose(res -> res < 0 ? CompletableFuture.failedFuture(new NativeCallException(NativeHelper.getErrorStr(-res))) : CompletableFuture.completedFuture(writeBuf.get(JAVA_LONG, 0)))
                .whenComplete((__, ___) -> {
                    shared.close();
                });
    }

    @Override
    protected int readFd() {
        return eventfd;
    }

}
