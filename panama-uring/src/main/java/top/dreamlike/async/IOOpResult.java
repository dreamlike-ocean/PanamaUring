package top.dreamlike.async;

import top.dreamlike.async.uring.Op;
import top.dreamlike.helper.BiIntConsumer;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Type;

public class IOOpResult {
    public final int fd;
    public int res;
    public  MemorySegment segment;

    public final BiIntConsumer callback;

    public int bid;

    public final Op op;

    public IOOpResult(int fd, int res,Op op, MemorySegment segment, BiIntConsumer callback) {
        this.fd = fd;
        this.res = res;
        this.segment = segment;
        this.callback = callback;
        this.op = op;
    }


    public void doCallBack(){
        callback.consumer(res,bid);
    }


    @Override
    public String toString() {
        return "IOOpResult{" +
                "fd=" + fd +
                ", res=" + res +
                '}';
    }
}