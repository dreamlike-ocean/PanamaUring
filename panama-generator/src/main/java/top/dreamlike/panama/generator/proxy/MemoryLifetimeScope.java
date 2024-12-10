package top.dreamlike.panama.generator.proxy;

import java.lang.foreign.Arena;
import java.lang.foreign.SegmentAllocator;
import java.util.concurrent.Callable;

public class MemoryLifetimeScope {
    static ThreadLocal<SegmentAllocator> currentAllocator = new ThreadLocal<>();
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
        try {
            currentAllocator.set(allocator);
            return callable.call();
        } finally {
            currentAllocator.remove();
        }
    }

    public void active(Runnable runnable) {
        try {
            currentAllocator.set(allocator);
            runnable.run();
        } finally {
            currentAllocator.remove();
        }
    }

}
