package top.dreamlike.async.file;


import top.dreamlike.async.PlainAsyncFd;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static top.dreamlike.nativeLib.unistd.unistd_h.pipe;

public class AsyncPipe extends PlainAsyncFd {
    int writeFd;
    int readFd;

    protected final AtomicBoolean closed = new AtomicBoolean(false);


    public AsyncPipe(IOUringEventLoop eventLoop) {
        super(eventLoop);
        try (Arena session = Arena.ofConfined()) {
            MemorySegment pipes = session.allocate(JAVA_INT, 2);
            int res = pipe(pipes);
            if (res == -1) {
                throw new NativeCallException(NativeHelper.getNowError());
            }
            writeFd = pipes.getAtIndex(ValueLayout.JAVA_INT, 1);
            readFd = pipes.getAtIndex(ValueLayout.JAVA_INT, 0);
        }

    }

    @Deprecated
    @Override
    public CompletableFuture<Integer> readUnsafe(int offset, MemorySegment memorySegment) {
        return super.readUnsafe(offset, memorySegment)
                .thenCompose(res -> res < 0 ? CompletableFuture.failedFuture(new NativeCallException(NativeHelper.getErrorStr(-res))) : CompletableFuture.completedFuture(res));
    }
    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }


    public boolean closed() {
        return closed.get();
    }

    @Override
    public int readFd() {
        return readFd;
    }

    @Override
    public int writeFd() {
        return writeFd;
    }

    @Override
    public String toString() {
        return STR. "AsyncPipe{ read fd: \{ readFd }, write fd: \{ writeFd } }" ;
    }
}
