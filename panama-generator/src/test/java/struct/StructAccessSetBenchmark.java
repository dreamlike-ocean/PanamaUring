package struct;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import top.dreamlike.panama.genertor.proxy.StructProxyGenerator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1000, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 100, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@State(Scope.Benchmark)
public class StructAccessSetBenchmark {

    private MemorySegment memorySegment;

    private EpollEventGenerated epollEventGenerated;

    private int[] data = new int[1024];

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + StructAccessSetBenchmark.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        memorySegment = Arena.global().allocate(epoll_event.$LAYOUT().byteSize());
        StructProxyGenerator generator = new StructProxyGenerator();
        epollEventGenerated = generator.enhance(memorySegment);
        for (int i = 0; i < data.length; i++) {
            data[i] = i * 3;
        }
    }

    @Benchmark
    public void setterGenerator() {
        for (int datum : data) {
            epollEventGenerated.setEvents(datum);
        }
    }

    @Benchmark
    public void setterLayout() {
        for (int datum : data) {
            epoll_event.events$set(memorySegment, datum);
        }
    }
}
