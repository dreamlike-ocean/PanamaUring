package top.dreamlike.async;

import top.dreamlike.async.uring.Op;
import top.dreamlike.helper.BiIntConsumer;

import java.lang.foreign.MemorySegment;

public class IOOpResult {
    public int fd;
    public int res;
    public MemorySegment segment;

    public final BiIntConsumer callback;

    public int bid;

    public final Op op;

    private IOOpResult(Op op, BiIntConsumer callback) {
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
        this.callback = callback;
        this.op = op;
    }

    public void doCallBack() {
       try {
           callback.consumer(res,bid);
       }catch (Throwable throwable){
           //ignore
       }
    }


    @Override
    public String toString() {
        return "IOOpResult{" +
                "fd=" + fd +
                ", res=" + res +
                '}';
    }
}