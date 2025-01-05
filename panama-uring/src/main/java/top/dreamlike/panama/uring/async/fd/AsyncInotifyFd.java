package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.Pair;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.inotify.NativeInotifyEvent;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static top.dreamlike.panama.uring.nativelib.libs.Libc.Inotify_H.IN_ACCESS;
import static top.dreamlike.panama.uring.nativelib.libs.Libc.Inotify_H.IN_OPEN;

public class AsyncInotifyFd implements IoUringAsyncFd {

    private final int inotifyFd;

    private final IoUringEventLoop eventLoop;

    public AsyncInotifyFd(IoUringEventLoop eventLoop) {
        int syscallRes = Instance.LIBC.inotify_init();
        if (syscallRes < 0) {
            throw new SyscallException(-NativeHelper.errorno());
        }
        this.inotifyFd = syscallRes;
        this.eventLoop = eventLoop;
    }

    public AsyncInotifyFd(IoUringEventLoop eventLoop, int flag) {
        int syscallRes = Instance.LIBC.inotify_init1(flag);
        if (syscallRes < 0) {
            throw new SyscallException(-NativeHelper.errorno());
        }
        this.inotifyFd = syscallRes;
        this.eventLoop = eventLoop;
    }

    public WatchKey register(Path path, int mask) {
        String name = path.toFile().getAbsolutePath();
        if (name.length() > 4 * 1024) {
            throw new IllegalArgumentException("path length must less than 4K");
        }

        var memoryAllocator = eventLoop.getMemoryAllocator();

        try (var arena = memoryAllocator.disposableArena()){
            MemorySegment namePtr = arena.allocateFrom(name);
            int wfd = NativeHelper.nativeCall(() -> Instance.LIBC.inotify_add_watch(inotifyFd, namePtr, mask));
            return new WatchKey(wfd, mask, name);
        }
    }

    public int unregister(WatchKey key) {
        return NativeHelper.nativeCall(() -> Instance.LIBC.inotify_rm_watch(inotifyFd, key.wfd));
    }

    public CancelableFuture<Pair<OwnershipMemory, List<InotifyEvent>>> asyncPoll(OwnershipMemory buffer) {
        return (CancelableFuture<Pair<OwnershipMemory, List<InotifyEvent>>>)
                asyncRead(buffer, (int) buffer.resource().byteSize(), 0)
                .thenCompose(res -> res.syscallRes() < 0 ? CompletableFuture.failedFuture(new SyscallException(res.syscallRes())) : CompletableFuture.completedFuture(res))
                .thenApply(res -> new Pair<>(buffer, parseEvents(res.buffer().resource(), res.syscallRes())));
    }

    private List<InotifyEvent> parseEvents(MemorySegment segment, int readLength) {
        ArrayList<InotifyEvent> list = new ArrayList<>();
        int offset = 0;
        while (offset < readLength) {
            MemorySegment event = segment.asSlice(offset);
            int wd = (int) NativeInotifyEvent.WD$VH.get(event, 0L);
            int mask = (int) NativeInotifyEvent.MASK$VH.get(event, 0L);
            int cookie = (int) NativeInotifyEvent.COOKIE$VH.get(event, 0L);
            int len = (int) NativeInotifyEvent.LEN$VH.get(event, 0L);
            String name = null;
            if (len != 0) {
                name = event.getString(NativeInotifyEvent.NAME_OFFSET);
            }
            list.add(new InotifyEvent(wd, mask, cookie, len, name));
            offset += (int) (len + NativeInotifyEvent.LAYOUT.byteSize());
        }
        return list;
    }

    @Override
    public IoUringEventLoop owner() {
        return eventLoop;
    }

    @Override
    public int fd() {
        return inotifyFd;
    }

    public record InotifyEvent(int wd, int mask, int cookie, int len, String name) {

        public boolean isOpen() {
            return (mask & IN_OPEN) != 0;
        }

        public boolean isAccess() {
            return (mask & IN_ACCESS) != 0;
        }

        public boolean isCreate() {
            return (mask & Libc.Inotify_H.IN_CREATE) != 0;
        }

        public boolean isDelete() {
            return (mask & Libc.Inotify_H.IN_DELETE) != 0;
        }

        public boolean isModify() {
            return (mask & Libc.Inotify_H.IN_MODIFY) != 0;
        }
    }

    public static class WatchKey {
        private final int wfd;
        private final int mask;
        private final String name;

        public WatchKey(int wfd, int mask, String name) {
            this.wfd = wfd;
            this.mask = mask;
            this.name = name;
        }

        public int getMask() {
            return mask;
        }

        public String getName() {
            return name;
        }
    }


}
