package top.dreamlike.async.file;

import io.smallrye.mutiny.Uni;
import top.dreamlike.async.PlainAsyncFd;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.memory.DefaultOwnershipMemory;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.CompletableFuture;

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
        Arena shared = Arena.ofShared();
        MemorySegment writeBuf = shared.allocate(JAVA_LONG, count);
        return writeUnsafe(0, writeBuf)
                .whenComplete((__, ___) -> {
                    shared.close();
                });
    }

    public Uni<Integer> writeLazy(long count) {
        Arena shared = Arena.ofShared();
        MemorySegment writeBuf = shared.allocate(JAVA_LONG, count);

        return writeUnsafeLazy(0,  new DefaultOwnershipMemory(writeBuf, shared))
                .onItem().invoke(p -> p.t1().drop())
                .onItem().transform(Pair::t2);
    }


    public CompletableFuture<Long> read() {
        Arena shared = Arena.ofShared();
        MemorySegment readByteBuf = shared.allocate(JAVA_LONG, 0);
        return readUnsafe(0, readByteBuf)
                .thenCompose(res -> res < 0 ? CompletableFuture.failedFuture(new NativeCallException(NativeHelper.getErrorStr(-res))) : CompletableFuture.completedFuture(readByteBuf.get(JAVA_LONG, 0)))
                .whenComplete((__, ___) -> {
                    shared.close();
                });
    }

    public Uni<Integer> readLazy() {
        Arena shared = Arena.ofShared();
        MemorySegment readByteBuf = shared.allocate(JAVA_LONG, 0);
        return readUnsafeLazy(0,  new DefaultOwnershipMemory(readByteBuf, shared))
                .onItem().invoke(p -> p.t1().drop())
                .onItem().transform(Pair::t2);
    }


    @Override
    public int readFd() {
        return eventfd;
    }

}
