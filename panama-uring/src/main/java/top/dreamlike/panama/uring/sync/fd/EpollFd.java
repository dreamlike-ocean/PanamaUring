package top.dreamlike.panama.uring.sync.fd;

import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.helper.EpollEvent;
import top.dreamlike.panama.uring.helper.MemoryAllocator;
import top.dreamlike.panama.uring.helper.Unsafe;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.struct.epoll.NativeEpollEvent;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EpollFd implements NativeFd, PollableFd {

    private final int epfd;

    private final int maxEvent;

    private final NativeArrayPointer<NativeEpollEvent> base;

    private final NativeEpollEvent nativeEpollEvent;

    private final OwnershipMemory rawEpollEventArray;

    private final OwnershipMemory rawEpollEventPtr;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final Lock ctlLock;

    public EpollFd(int flag, int maxEvent) {
        int epoll_create = Instance.LIB_EPOLL.epoll_create(flag);
        if (epoll_create < 0) {
            throw new IllegalArgumentException("epoll_create failed, error: " + NativeHelper.currentErrorStr());
        }
        this.epfd = epoll_create;
        this.maxEvent = maxEvent;
        this.rawEpollEventArray = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(maxEvent * NativeEpollEvent.LAYOUT.byteSize());
        this.rawEpollEventPtr = MemoryAllocator.LIBC_MALLOC.allocateOwnerShipMemory(NativeEpollEvent.LAYOUT.byteSize());
        this.base = new NativeArrayPointer<>(Instance.STRUCT_PROXY_GENERATOR, rawEpollEventArray.resource());
        this.nativeEpollEvent = Instance.STRUCT_PROXY_GENERATOR.enhance(rawEpollEventPtr.resource());
        this.ctlLock = new ReentrantLock();
    }

    public EpollFd() {
        this(0, 32);
    }

    public int epollCtl(PollableFd targetFd, int op, EpollEvent event) {
        if (closed.get()) {
            throw new IllegalArgumentException("epoll fd is closed");
        }
        ctlLock.lock();

        try {
            MemorySegment nativeStruct = rawEpollEventPtr.resource();
            NativeEpollEvent.U64_VH.set(nativeStruct, 0L, event.data());
            NativeEpollEvent.EVENTS_VH.set(nativeStruct, 0L, event.events());
            return Instance.LIB_EPOLL.epoll_ctl(epfd, op, targetFd.fd(), this.nativeEpollEvent);
        } finally {
            ctlLock.unlock();
        }

    }

    @Unsafe("Not Thread Safe")
    public List<EpollEvent> epollWait(int maxEvents, int timeout, TimeUnit unit) {
        int waitResult = Instance.LIB_EPOLL.epoll_wait(epfd, base, maxEvents, (int) unit.toMillis(timeout));
        maxEvents = Math.min(maxEvents, this.maxEvent);
        if (waitResult < 0) {
            throw new IllegalArgumentException("epoll_wait failed, error: " + NativeHelper.currentErrorStr());
        }
        if (waitResult == 0) {
            return List.of();
        }
        ArrayList<EpollEvent> list = new ArrayList<>(waitResult);
        for (int i = 0; i < waitResult; i++) {
            NativeEpollEvent nativeEvent = base.getAtIndex(i);
            EpollEvent javaEvent = new EpollEvent(nativeEvent.getEvents(), nativeEvent.getU64());
            list.add(javaEvent);
        }
        return list;
    }


    @Override
    public int fd() {
        return epfd;
    }

    @Override
    public int writeFd() {
        throw new UnsupportedOperationException("epoll fd is not writable");
    }

    @Override
    public int readFd() {
        throw new UnsupportedOperationException("epoll fd is not readable");
    }


    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Instance.LIBC.close(epfd);
            rawEpollEventArray.drop();
            rawEpollEventPtr.drop();
        }
    }
}
