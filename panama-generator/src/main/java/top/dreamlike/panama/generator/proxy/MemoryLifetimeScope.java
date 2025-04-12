package top.dreamlike.panama.generator.proxy;

import java.lang.foreign.Arena;
import java.lang.foreign.SegmentAllocator;
import java.util.Objects;
import java.util.concurrent.Callable;

public class MemoryLifetimeScope implements AutoCloseable{
    static ThreadLocal<MemoryLifetimeScope> currentScope = new ThreadLocal<>();
    final SegmentAllocator allocator;
    final boolean needClose;

    private MemoryLifetimeScope(SegmentAllocator allocator, boolean needClose) {
        this.allocator = allocator;
        this.needClose = needClose;
        currentScope.set(this);
    }

    static MemoryLifetimeScope currentScope() {
        MemoryLifetimeScope memoryLifetimeScope = MemoryLifetimeScope.currentScope.get();
        if (memoryLifetimeScope == null) {
            throw new IllegalArgumentException("you should open a MemoryLifetimeScope first,please use MemoryLifetimeScope.auto(), MemoryLifetimeScope.local() or MemoryLifetimeScope.of(...)");
        }
        return memoryLifetimeScope;
    }

    public static MemoryLifetimeScope of(SegmentAllocator allocator) {
        Objects.requireNonNull(allocator);
        return new MemoryLifetimeScope(allocator, false);
    }

    public static MemoryLifetimeScope auto() {
        return new MemoryLifetimeScope(Arena.ofAuto(), true);
    }

    public static MemoryLifetimeScope local() {
        return new MemoryLifetimeScope(Arena.ofConfined(), true);
    }

    public <T> T active(Callable<T> callable) throws Exception {
        try (MemoryLifetimeScope _ = this) {
            return callable.call();
        }
    }

    public void active(Runnable runnable) {
        try (MemoryLifetimeScope _ = this) {
            runnable.run();
        } catch (Throwable throwable) {
            throw new RuntimeException("catch error,rethrow!", throwable);
        }
    }

    @Override
    public void close() throws Exception {
        MemoryLifetimeScope scope = currentScope.get();
        currentScope.remove();
        if (scope.needClose && scope instanceof Arena allocatorClosable) {
            allocatorClosable.close();
        }
    }
}
