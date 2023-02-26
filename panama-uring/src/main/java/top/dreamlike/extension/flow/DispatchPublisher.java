package top.dreamlike.extension.flow;

import java.util.concurrent.Executor;

public class DispatchPublisher<T> extends SimplePublisher<T> {

    private final Executor dispatcher;

    public DispatchPublisher(int maxBuffer, Runnable onCancel, Executor executor) {
        super(maxBuffer, onCancel);
        this.dispatcher = executor;
    }

    @Override
    public void sendError(Throwable throwable) {
        dispatcher.execute(() -> super.sendError(throwable));
    }


    @Override
    public void request(long n) {
        dispatcher.execute(() -> super.request(n));
    }

    @Override
    protected void sendEvent(T event) {
        dispatcher.execute(() -> super.sendEvent(event));
    }

    @Override
    public void end() {
        dispatcher.execute(super::end);
    }
}
