package top.dreamlike.async;

import top.dreamlike.helper.BiIntConsumer;

import java.lang.foreign.MemorySegment;

public class IOOpResult {
    public final int fd;
    public int res;
    public  MemorySegment segment;

    public BiIntConsumer callback;

    public int bid;

    public IOOpResult(int fd, int res, MemorySegment segment, BiIntConsumer callback) {
        this.fd = fd;
        this.res = res;
        this.segment = segment;
        this.callback = callback;
    }




    @Override
    public String toString() {
        return "IOOpResult{" +
                "fd=" + fd +
                ", res=" + res +
                '}';
    }
}