package top.dreamlike.extension.flow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * 单线程且只接受一次Subscribe发布者
 *
 * @param <T>
 */
public class SimplePublisher<T> implements Flow.Publisher<T>, Flow.Subscription {

    protected Flow.Subscriber<? super T> subscriber;

    protected Runnable onCancel;

    protected volatile int maxBuffer;

    protected int demand;

    protected final Deque<T> buffer;

    private boolean cancel = false;

    public SimplePublisher(int maxBuffer, Runnable onCancel) {
        this.onCancel = onCancel;
        this.maxBuffer = maxBuffer;
        this.buffer = new ArrayDeque<>();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalArgumentException("Only one subscriber is allowed to register");
        }
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }


    @Override
    public void request(long n) {
        while (!buffer.isEmpty() && n > 0) {
            T pop = buffer.pop();
            sendEvent(pop);
            n--;
        }
        if (n > 0) {
            demand += n;
        }
    }

    @Override
    public void cancel() {
        if (cancel) {
            return;
        }
        onCancel.run();
        cancel = !cancel;
    }

    public void sendError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    public List<T> fetchBuffers() {
        return new ArrayList<>(buffer);
    }

    /**
     * @param t item
     * @return 是否
     */
    public boolean offer(T t) {
        if (demand > 0) {
            sendEvent(t);
            demand--;
            return true;
        }
        buffer.offer(t);
        return buffer.size() != maxBuffer;
    }


    public void end() {
        subscriber.onComplete();
    }

    public boolean isCancel() {
        return cancel;
    }

    protected void sendEvent(T event) {
        subscriber.onNext(event);
    }
}
