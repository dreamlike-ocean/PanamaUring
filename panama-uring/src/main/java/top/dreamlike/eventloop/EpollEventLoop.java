package top.dreamlike.eventloop;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.epoll.extension.ListenContext;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;
import top.dreamlike.nativeLib.epoll.epoll_h;
import top.dreamlike.nativeLib.eventfd.EventFd;
import top.dreamlike.thirdparty.colletion.IntObjectHashMap;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;

public class EpollEventLoop extends BaseEventLoop {
    private final Epoll epoll;

    private final Queue<Epoll.Event> processingEvent;

    private final static ListenContext EMPTY = new ListenContext(0, 0, (__) -> {
    });
    private final IntObjectHashMap<ListenContext> fdHandler;

    private final EventFd wakeUpFd;

    public EpollEventLoop() {
        epoll = new Epoll();
        processingEvent = new ArrayDeque<>();
        fdHandler = new IntObjectHashMap<>();
        wakeUpFd = new EventFd();
        wakeUpFd.transToNoBlock();
        registerEvent0(wakeUpFd.getFd(), EPOLLIN(), (__) -> {
            long readSync = wakeUpFd.readSync();
        });
    }

    /**
     * @param fd        需要监听的fd
     * @param eventMask 事件
     * @param callback  当epoll被触发时给出的event处理程序的回调
     * @return 注册结果 要切线程
     */
    public CompletableFuture<Void> registerEvent(int fd, int eventMask, IntConsumer callback) {
        return runOnEventLoop(() -> registerEvent0(fd, eventMask, callback))
                .thenCompose(NativeHelper::errorNoTransform);
    }

    private int registerEvent0(int fd, int eventMask, IntConsumer callback) {
        int register = epoll.register(fd, eventMask);

        if (register == 0) {
            fdHandler.put(fd, new ListenContext(fd, eventMask, callback));
        }
        return register;
    }

    public CompletableFuture<Void> modifyCallBack(int fd, IntConsumer callback) {
        return runOnEventLoop(() -> {
            ListenContext oldValue = fdHandler.get(fd);
            if (oldValue == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            fdHandler.put(fd, new ListenContext(fd, oldValue.eventMask(), callback));
            return null;
        });
    }

    public int fetchFdEventMask(int fd) {
        return fdHandler.getOrDefault(fd, EMPTY).eventMask();
    }

    public CompletableFuture<Void> addEvent(int fd, int event) {
        return runOnEventLoop(() -> {
            ListenContext listenContext = fdHandler.get(fd);
            if (listenContext == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            //这个掩码已经注册 直接不做
            int oldMask = listenContext.eventMask();
            if ((oldMask & event) != 0) {
                return 0;
            }
            int eventMask = oldMask | event;
            return modifyEvent0(fd, eventMask, listenContext);
        }).thenCompose(NativeHelper::errorNoTransform);
    }

    public CompletableFuture<Void> removeEvent(int fd, int event) {
        return runOnEventLoop(() -> removeEventUnsafe(fd, event)).thenCompose(NativeHelper::errorNoTransform);
    }

    public int removeEventUnsafe(int fd, int event) {
        ListenContext listenContext = fdHandler.get(fd);
        if (listenContext == null) {
            throw new NativeCallException("res need to be registered to epoll");
        }
        //这个掩码没注册 直接不做
        int oldMask = listenContext.eventMask();
        if ((oldMask & event) == 0) {
            return 0;
        }
        int eventMask = oldMask & ~event;
        return modifyEvent0(fd, eventMask, listenContext);
    }


    public CompletableFuture<Void> modifyAll(int fd, int eventMask, IntConsumer callback) {
        return runOnEventLoop(() -> {
            if (fdHandler.get(fd) == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            fdHandler.put(fd, new ListenContext(fd, eventMask, callback));
            return epoll.modify(fd, eventMask);
        }).thenCompose(NativeHelper::errorNoTransform);
    }


    public CompletableFuture<Void> modifyEvent(int fd, int event) {
        return runOnEventLoop(() -> {
            ListenContext listenContext = fdHandler.get(fd);
            if (listenContext == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            return modifyEvent0(fd, event, listenContext);
        }).thenCompose(NativeHelper::errorNoTransform);
    }

    private int modifyEvent0(int fd, int event, ListenContext listenContext) {
        int res = epoll.modify(fd, event);
        if (res == 0) {
            fdHandler.put(fd, new ListenContext(fd, event, listenContext.callback()));
        }
        return res;
    }

    public CompletableFuture<Void> unregisterEvent(int fd) {
        return runOnEventLoop(() -> {
            if (fdHandler.remove(fd) == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            return epoll.unRegister(fd);
        }).thenCompose(NativeHelper::errorNoTransform);
    }


    @Override
    public void wakeup() {
        wakeUpFd.write(1);
    }

    @Override
    protected void selectAndWait(long duration) {
        processingEvent.addAll(epoll.select((int) duration));
    }

    @Override
    protected void close0() throws Exception {
        epoll.close();
        wakeUpFd.close();
    }

    @Override
    protected void afterSelect() {
        while (!processingEvent.isEmpty()) {
            Epoll.Event event = processingEvent.poll();
            var eventHandler = (event.event() & epoll_h.EPOLLONESHOT()) != 0 ? fdHandler.remove(event.fd()) : fdHandler.get(event.fd());
            if (eventHandler != null) {
                eventHandler.callback().accept(event.event());
            }
        }
    }

    static {
//        AccessHelper.setEpollEventLoopTasks = EpollEventLoop::setTaskQueue;
        AccessHelper.setEpollEventLoopTasks = ((eventLoop, tasks) -> eventLoop.tasks = tasks);
        AccessHelper.registerToEpollDirectly = (EpollEventLoop loop, Pair<Epoll.Event, IntConsumer> p) -> {
            Epoll.Event event = p.t1();
            loop.registerEvent0(event.fd(), event.event(), p.t2());
        };
    }
}
