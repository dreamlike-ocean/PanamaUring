package top.dreamlike.panama.uring.networking.eventloop;

import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongFunction;

public class ReadEventLoopOption extends IoUringEventLoopOption<ReadEventLoopOption> {
    private Function<IoUringEventLoop, LongFunction<IoUringBufferRing>> chooser = ReaderEventLoop.DefaultBufferRingChoose::new;

    public Function<IoUringEventLoop, LongFunction<IoUringBufferRing>> getChooser() {
        return chooser;
    }

    public ReadEventLoopOption setChooser(Function<IoUringEventLoop, LongFunction<IoUringBufferRing>> chooser) {
        Objects.requireNonNull(chooser);
        this.chooser = chooser;
        return this;
    }

}
