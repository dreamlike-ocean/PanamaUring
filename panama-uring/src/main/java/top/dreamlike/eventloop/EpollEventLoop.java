package top.dreamlike.eventloop;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.helper.NativeCallException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.epoll.epoll_h;
import top.dreamlike.nativeLib.eventfd.EventFd;
import top.dreamlike.thirdparty.colletion.IntObjectHashMap;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

public class EpollEventLoop extends BaseEventLoop {
    private final Epoll epoll;

    private final ArrayList<Epoll.Event> processingEvent;

    private final IntObjectHashMap<IntConsumer> fdHandler;

    private final EventFd eventFd;

    public EpollEventLoop() {
        epoll = new Epoll();
        processingEvent = new ArrayList<>();
        fdHandler = new IntObjectHashMap<>();
        eventFd = new EventFd();
    }

    /**
     * @param fd       需要监听的fd
     * @param event    事件
     * @param callback 当epoll被触发时给出的event处理程序的回调
     * @return 注册结果 要切线程
     */
    public CompletableFuture<Void> registerEvent(int fd, int event, IntConsumer callback) {
        return runOnEventLoop(() -> {
            int register = epoll.register(fd, event);

            if (register == 0) {
                fdHandler.put(fd, callback);
            }
            return register;
        })
                .thenCompose(NativeHelper::errorNoTransform);
    }

    public CompletableFuture<Void> modifyCallBack(int fd, IntConsumer callback) {
        return runOnEventLoop(() -> {
            if (fdHandler.get(fd) == null) {
                throw new NativeCallException("fd need to be registered to epoll");
            }
            fdHandler.put(fd, callback);
            return null;
        });
    }

    public CompletableFuture<Void> modifyEvent(int fd, int event) {
        return runOnEventLoop(() -> {
            if (fdHandler.get(fd) == null) {
                throw new NativeCallException("fd need to be registered to epoll");
            }
            return epoll.modify(fd, event);
        }).thenCompose(NativeHelper::errorNoTransform);
    }

    public CompletableFuture<Void> unregisterEvent(int fd) {
        return runOnEventLoop(() -> {
            if (fdHandler.remove(fd) == null) {
                throw new NativeCallException("fd need to be registered to epoll");
            }
            return epoll.unRegister(fd);
        }).thenCompose(NativeHelper::errorNoTransform);
    }


    @Override
    public void wakeup() {
        eventFd.write(1);
    }

    @Override
    protected void selectAndWait(long duration) {
        processingEvent.addAll(epoll.select((int) duration));
    }

    @Override
    protected void close0() throws Exception {
        epoll.close();
        eventFd.close();
    }

    @Override
    protected void afterSelect() {
        for (Epoll.Event event : processingEvent) {
            var eventHandler = (event.event() | epoll_h.EPOLLONESHOT()) != 0 ? fdHandler.remove(event.fd()) : fdHandler.get(event.fd());
            if (eventHandler != null) {
                eventHandler.accept(event.event());
            }
        }
    }

    static {
//        AccessHelper.setEpollEventLoopTasks = EpollEventLoop::setTaskQueue;
        AccessHelper.setEpollEventLoopTasks = ((eventLoop, tasks) -> eventLoop.tasks = tasks);
    }
}
