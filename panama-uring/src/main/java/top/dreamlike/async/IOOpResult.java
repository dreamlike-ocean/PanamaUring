package top.dreamlike.async;

import top.dreamlike.async.uring.Op;
import top.dreamlike.helper.BiIntConsumer;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public class IOOpResult {
    public int fd;
    public int res;
    public MemorySegment segment;

//    public BiIntConsumer callback;

    public final Consumer<IOOpResult> callback;

    public int bid;

    public final Op op;

    public int flag;

    public long userData;

    private IOOpResult(Op op, BiIntConsumer callback) {
        this.callback = (__) -> callback.consumer(res, bid);
        this.op = op;
    }

    private IOOpResult(Op op, Consumer<IOOpResult> callback) {
        this.callback = callback;
        this.op = op;
    }


    public static IOOpResult bindCallBack(Op op, BiIntConsumer callback) {
        return new IOOpResult(op, callback);
    }

    public IOOpResult(int fd, int res, Op op, MemorySegment segment, BiIntConsumer callback) {
        this.fd = fd;
        this.res = res;
        this.segment = segment;
        this.callback = (__) -> callback.consumer(res, bid);
        this.op = op;
    }

    public static IOOpResult bindCallBack(Op op, Consumer<IOOpResult> callback) {
        return new IOOpResult(op, callback);
    }

    public void doCallBack() {
        try {
            callback.accept(this);
        } catch (Throwable throwable) {
            //ignore
        }
    }

    @Override
    public String toString() {
        return "IOOpResult{" +
                "res=" + fd +
                ", res=" + res +
                '}';
    }
}