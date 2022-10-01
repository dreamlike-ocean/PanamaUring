package top.dreamlike.asyncFile;

import java.lang.foreign.MemorySegment;
import java.util.function.IntConsumer;

public class IOOpResult{
    public final int fd;
    public int res;
    public final MemorySegment segment;

    public IntConsumer callback;

    public IOOpResult(int fd, int res, MemorySegment segment,IntConsumer callback) {
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
                ", segment=" + segment +
                '}';
    }
}