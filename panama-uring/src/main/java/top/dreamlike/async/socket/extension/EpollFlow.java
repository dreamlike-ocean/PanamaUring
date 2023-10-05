package top.dreamlike.async.socket.extension;

import top.dreamlike.eventloop.EpollUringEventLoop;
import top.dreamlike.extension.flow.DispatchPublisher;
import top.dreamlike.extension.flow.SimpleSubscriber;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;

public class EpollFlow<T> extends DispatchPublisher<T> implements Flow.Subscriber<T> {
    private final SimpleSubscriber<T> simpleSubscriber;
    private final EpollUringEventLoop eventLoop;
    private final int fd;

    public EpollFlow(int maxBuffer, int fd, EpollUringEventLoop epollUringEventLoop, Consumer<T> onNext) {
        super(maxBuffer, null, epollUringEventLoop);
        this.eventLoop = epollUringEventLoop;
        this.onCancel = this::cancelCallback;
        this.simpleSubscriber = new SimpleSubscriber<>();
        this.simpleSubscriber.setConsumer(onNext);
        this.fd = fd;
        this.subscribe(simpleSubscriber);
    }

    public void cancelCallback() {
        eventLoop.runOnEventLoop(() -> {
            eventLoop.epollMode().removeEventUnsafe(fd, EPOLLIN());
        });
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        simpleSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        simpleSubscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        simpleSubscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        simpleSubscriber.onComplete();
    }
}
