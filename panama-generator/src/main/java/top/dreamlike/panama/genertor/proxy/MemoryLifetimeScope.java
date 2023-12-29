package top.dreamlike.panama.genertor.proxy;

import java.lang.foreign.Arena;
import java.lang.foreign.SegmentAllocator;
import java.util.concurrent.Callable;

public class MemoryLifetimeScope {
    static ScopedValue<SegmentAllocator> currentAllocator = ScopedValue.newInstance();
    final SegmentAllocator allocator;

    private MemoryLifetimeScope(SegmentAllocator allocator) {
        this.allocator = allocator;
    }

    public static MemoryLifetimeScope of(SegmentAllocator allocator) {
        return allocator == null
                ? auto()
                : new MemoryLifetimeScope(allocator);
    }

    public static MemoryLifetimeScope auto() {
        return new MemoryLifetimeScope(Arena.ofAuto());
    }

    public <T> T active(Callable<T> callable) throws Exception {
        return ScopedValue.where(currentAllocator, allocator)
                .call(callable);
    }

    public void active(Runnable runnable) {
        ScopedValue.where(currentAllocator, allocator)
                .run(runnable);
    }

}
