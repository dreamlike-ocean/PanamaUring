package top.dreamlike.fileSystem;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.helper.FileEvent;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Unsafe;
import top.dreamlike.nativeLib.inotify.inotify_event;
import top.dreamlike.nativeLib.inotify.inotify_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.inotify.inotify_h.inotify_add_watch;
import static top.dreamlike.nativeLib.inotify.inotify_h.inotify_rm_watch;
import static top.dreamlike.nativeLib.unistd.unistd_h.read;

@Unsafe("不保证线程安全")
public class WatchService {

    static {
        AccessHelper.fetchINotifyFd = (w) -> w.ifd;
        AccessHelper.fetchBuffer = w -> w.buf;
    }

    private final int ifd;
    private final MemorySession memorySession;
    private final MemorySegment buf;
    private final AtomicBoolean nonBlock = new AtomicBoolean(false);

    public WatchService() {
        ifd = inotify_h.inotify_init();
        if (ifd <= 0) {
            throw new NativeCallException(NativeHelper.getNowError());
        }
        memorySession = MemorySession.openShared();
        buf = memorySession.allocate(4048);

    }

    @Unsafe("内部使用 确保buf可用")
    public static List<FileEvent> selectEventInternal(MemorySegment buf, int readLength) {
        ArrayList<FileEvent> res = new ArrayList<>(4);
        int offset = 0;
        while (offset < readLength) {
            MemorySegment event = buf.asSlice(offset);
            int mask = inotify_event.mask$get(event);
            int wfd = inotify_event.wd$get(event);
            String name = inotify_event.name$get(event);
            res.add(new FileEvent(mask, name, wfd));
            offset += inotify_event.len$get(event) + inotify_event.sizeof();
        }
        return res;
    }

    public void registerToEpoll(Epoll epoll) {
        int res = epoll.register(ifd, EPOLLIN());
        if (res < 0)
            throw new NativeCallException(NativeHelper.getNowError());
    }

    public void unRegisterToEpoll(Epoll epoll) {
        int res = epoll.unRegister(ifd);
        if (res < 0)
            throw new NativeCallException(NativeHelper.getNowError());
    }

    public int register(Path path, int mask) {
        String absolutePath = path.toFile().getAbsolutePath();
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment file = session.allocateUtf8String(absolutePath);
            int res = inotify_add_watch(ifd, file, mask);
            if (res <= 0) {
                throw new NativeCallException(NativeHelper.getNowError());
            }
            return res;
        }
    }

    public void removeListen(int wd) {
        int res = inotify_rm_watch(ifd, wd);
        if (res <= 0)
            throw new NativeCallException(NativeHelper.getNowError());
    }

    public void makeNoBlock() {
        if (nonBlock.get() || !nonBlock.compareAndSet(false, true)) {
            return;
        }
        int res = NativeHelper.makeNoBlocking(ifd);
        if (res < 0) {
            nonBlock.compareAndSet(true, false);
            throw new NativeCallException(NativeHelper.getErrorStr(-res));
        }
    }

    public List<FileEvent> selectEvent() {
        return selectEventInternal(buf, (int) read(ifd, buf, buf.byteSize()));
    }
}
