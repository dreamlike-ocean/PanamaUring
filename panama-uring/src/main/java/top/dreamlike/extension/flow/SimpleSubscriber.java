package top.dreamlike.extension.flow;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class SimpleSubscriber<T> implements Flow.Subscriber<T> {

    private Flow.Subscription subscription;

    private final boolean cancel = false;

    // 默认drop
    private Consumer<T> onNext = (t) -> {
    };

    private final Runnable onComplete = () -> {
    };

    private final Consumer<Throwable> onError = (t) -> {
    };

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(T item) {
        this.onNext.accept(item);
    }

    @Override
    public void onError(Throwable throwable) {
        onError.accept(throwable);
    }

    @Override
    public void onComplete() {
        onComplete.run();
    }

    public void setConsumer(Consumer<T> consumer) {
        this.onNext = consumer;
    }

}
