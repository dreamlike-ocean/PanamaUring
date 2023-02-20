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

    private Flow.Subscriber<? super T> subscriber;

    private final Runnable onCancel;

    private int maxBuffer;

    private int demand;

    private final Deque<T> buffer;

    private boolean cancel = false;

    public SimplePublisher(int maxBuffer, Runnable onCancel) {
        this.onCancel = onCancel;
        this.maxBuffer = maxBuffer;
        this.buffer = new ArrayDeque<>();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
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
            subscriber.onNext(pop);
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

    public List<T> fetchBuffers() {
        return new ArrayList<>(buffer);
    }

    /**
     * @param t item
     * @return 是否
     */
    public boolean offer(T t) {
        if (cancel) {
            return false;
        }
        if (demand > 0) {
            subscriber.onNext(t);
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
}
