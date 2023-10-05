package top.dreamlike.eventloop;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.epoll.extension.ListenContext;
import top.dreamlike.epoll.extension.ListenContextFactory;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.Pair;
import top.dreamlike.helper.Unsafe;
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

    private final IntObjectHashMap<ListenContext> fdHandler;

    private final EventFd wakeUpFd;

    static {
//        AccessHelper.setEpollEventLoopTasks = EpollEventLoop::setTaskQueue;
        AccessHelper.setEpollEventLoopTasks = ((eventLoop, tasks) -> eventLoop.tasks = tasks);
        AccessHelper.registerToEpollDirectly = (EpollEventLoop loop, Pair<Epoll.Event, IntConsumer> p) -> {
            Epoll.Event event = p.t1();
            loop.registerReadEvent0(event.fd(), event.event(), p.t2());
        };
    }

    public EpollEventLoop() {
        epoll = new Epoll();
        processingEvent = new ArrayDeque<>();
        fdHandler = new IntObjectHashMap<>();
        wakeUpFd = new EventFd();
        wakeUpFd.transToNoBlock();
        registerReadEvent0(wakeUpFd.getFd(), EPOLLIN(), (__) -> {
            long readSync = wakeUpFd.readSync();
        });
    }

    /**
     * @param fd        需要监听的fd
     * @param eventMask 事件
     * @param readCallback  当epoll被触发时给出的event处理程序的回调
     * @return 注册结果 要切线程
     */
    public CompletableFuture<Void> registerReadEvent(int fd, int eventMask, IntConsumer readCallback) {
        return runOnEventLoop(() -> registerReadEvent0(fd, eventMask, readCallback))
                .thenCompose(NativeHelper::errorNoTransform);
    }

    public CompletableFuture<Void> registerWriteEvent(int fd, int eventMask, IntConsumer writeCallback) {
        return runOnEventLoop(() -> registerWriteEvent0(fd, eventMask, writeCallback))
                .thenCompose(NativeHelper::errorNoTransform);
    }

    private int registerReadEvent0(int fd, int eventMask, IntConsumer readCallback) {
        int register = epoll.register(fd, eventMask);
        if (register == 0) {
            ListenContext context = fdHandler.get(fd);
            if (context == null) {
                context = ListenContextFactory.readMode(fd, eventMask, readCallback);
            } else {
                context = ListenContextFactory.merge(ListenContextFactory.readMode(fd, eventMask, readCallback), context);
            }
            fdHandler.put(fd, context);
        }
        return register;
    }

    private int registerWriteEvent0(int fd, int eventMask, IntConsumer writeCallback) {
        int register = epoll.register(fd, eventMask);
        if (register == 0) {
            ListenContext context = fdHandler.get(fd);
            if (context == null) {
                context = ListenContextFactory.writeMode(fd, eventMask, writeCallback);
            } else {
                context = ListenContextFactory.merge(ListenContextFactory.writeMode(fd, eventMask, writeCallback), context);
            }
            fdHandler.put(fd, context);
        }
        return register;
    }

    public CompletableFuture<Void> modifyReadCallBack(int fd, IntConsumer readCallback) {
        return runOnEventLoop(() -> {
            ListenContext oldValue = fdHandler.get(fd);
            if (oldValue == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            fdHandler.put(fd, ListenContextFactory.readMode(fd, oldValue.eventMask(), readCallback));
            return null;
        });
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
            return modifyEventUnsafe(fd, eventMask, listenContext);
        }).thenCompose(NativeHelper::errorNoTransform);
    }

    public int fetchFdEventMask(int fd) {
        return fdHandler.getOrDefault(fd, ListenContextFactory.EMPTY).eventMask();
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
        return modifyEventUnsafe(fd, eventMask, listenContext);
    }

    public CompletableFuture<Void> modifyEvent(int fd, int event) {
        return runOnEventLoop(() -> {
            ListenContext listenContext = fdHandler.get(fd);
            if (listenContext == null) {
                throw new NativeCallException("res need to be registered to epoll");
            }
            return modifyEventUnsafe(fd, event, listenContext);
        }).thenCompose(NativeHelper::errorNoTransform);
    }

    public CompletableFuture<Void> removeEvent(int fd, int event) {
        return runOnEventLoop(() -> removeEventUnsafe(fd, event))
                .thenCompose(NativeHelper::errorNoTransform);
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
    public void wakeup0() {
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

    @Unsafe("自行保证线程安全")
    public int modifyEventUnsafe(int fd, int currentEventMask, ListenContext oldListenContext) {
        int res = epoll.modify(fd, currentEventMask);
        if (res == 0) {
            ListenContext expectListenContext = oldListenContext;
            if ((currentEventMask & epoll_h.EPOLLIN()) == 0) {
                expectListenContext = expectListenContext.removeReadCallback();
            }
            if ((currentEventMask & epoll_h.EPOLLOUT()) == 0) {
                expectListenContext = expectListenContext.removeWriteCallback();
            }
            fdHandler.put(fd, expectListenContext);
        }
        return res;
    }

    @Override
    protected void afterSelect() {
        while (!processingEvent.isEmpty()) {
            Epoll.Event event = processingEvent.poll();
            int eventMask = event.event();
            var eventHandler = (eventMask & epoll_h.EPOLLONESHOT()) != 0 ? fdHandler.remove(event.fd()) : fdHandler.get(event.fd());
            if (eventHandler != null) {
                if ((eventMask & epoll_h.EPOLLIN()) != 0) {
                    eventHandler.readCallback().accept(eventMask);
                }

                if ((eventMask & epoll_h.EPOLLOUT()) != 0) {
                    eventHandler.writeCallback().accept(eventMask);
                }
            }
        }
    }
}
