package top.dreamlike.async.socket.extension;

import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.flow.DispatchPublisher;
import top.dreamlike.extension.flow.SimpleSubscriber;
import top.dreamlike.helper.NativeCallException;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class IOUringFlow<T> extends DispatchPublisher<T> implements Flow.Subscriber<T> {

    private final IOUringEventLoop eventLoop;

    private final SimpleSubscriber<T> simpleSubscriber;


    private long userData = -1;

    public IOUringFlow(int maxBuffer, IOUringEventLoop eventLoop, Consumer<T> onNext) {
        //给null的原因是不许 this::cancelCallback
        super(maxBuffer, null, eventLoop);
        this.eventLoop = eventLoop;
        this.simpleSubscriber = new SimpleSubscriber<>();
        this.onCancel = this::cancelCallback;
        simpleSubscriber.setConsumer(onNext);
        this.subscribe(simpleSubscriber);
    }

    public void setUserData(long userData) {
        if (this.userData != -1) {
            return;
        }
        this.userData = userData;
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


    private void cancelCallback() {
        if (userData == -1) {
            throw new IllegalStateException("async op not submit");
        }
        eventLoop.cancel(userData, 0)
                .whenComplete((res, throwable) -> {
                    if (res != null) {
                        end();
                        return;
                    }
                    sendError(new NativeCallException("flow cancel fail", throwable));
                });
    }


}
