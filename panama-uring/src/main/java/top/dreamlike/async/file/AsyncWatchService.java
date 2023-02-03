package top.dreamlike.async.file;

import top.dreamlike.FileSystem.WatchService;
import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.FileEvent;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


non-sealed public class AsyncWatchService extends AsyncFd {
    private final IOUring uring;

    private final WatchService watchService;

    private final IOUringEventLoop eventLoop;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final int ifd;

    public AsyncWatchService(IOUringEventLoop eventLoop) {
        super(eventLoop);
        this.watchService = new WatchService();
        watchService.makeNoBlock();
        this.ifd = AccessHelper.fetchINotifyFd.apply(watchService);
        this.eventLoop = eventLoop;
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }

    public CompletableFuture<Integer> register(Path path, int mask) {
        if (closed()) {
            throw new IllegalStateException("service has close");
        }
        return eventLoop.runOnEventLoop(() -> watchService.register(path, mask));
    }

    public CompletableFuture<Void> unRegister(int wd) {
        if (closed()) {
            throw new IllegalStateException("service has close");
        }
        return eventLoop.runOnEventLoop(() -> {
            watchService.removeListen(wd);
            return null;
        });
    }


    public synchronized CompletableFuture<List<FileEvent>> select() {
        var future = new CompletableFuture<List<FileEvent>>();
        List<FileEvent> fastRes = watchService.selectEvent();
        //立刻读一下 快速路径
        if (!fastRes.isEmpty()) {
            future.complete(fastRes);
            return future;
        }
        eventLoop.runOnEventLoop(() -> {
            MemorySegment buf = AccessHelper.fetchBuffer.apply(watchService);
            boolean prep_res = uring.prep_read(ifd, 0, buf, length -> {
                if (length < 0) {
                    future.completeExceptionally(new NativeCallException(NativeHelper.getErrorStr(-length)));
                    return;
                }
                List<FileEvent> fileEvents = WatchService.selectEventInternal(buf, length);
                future.complete(fileEvents);
            });
            if (!prep_res) {
                future.completeExceptionally(new Exception("没有空闲的sqe"));
            }
        });
        return future;
    }

    public boolean closed() {
        return closed.get();
    }
}
