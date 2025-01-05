package top.dreamlike.panama.uring.helper;

import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface MemoryAllocator<T extends OwnershipMemory> {

    long MAX_MALLOC_ALIGN = ValueLayout.ADDRESS.byteSize() == 4 ? 8 : 16;

    T allocateOwnerShipMemory(long size);

    default void free(T memory) {
        memory.drop();
    }

    default Arena  disposableArena() {
        return new Arena() {
            private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
            private final ConcurrentLinkedQueue<OwnershipMemory> memoryQueue = new ConcurrentLinkedQueue<>();

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                long alignedSize = Math.max(1L, byteAlignment > MAX_MALLOC_ALIGN ? byteSize + (byteAlignment - 1) : byteSize);
                if (reentrantReadWriteLock.readLock().tryLock()) {
                    try {
                        T ownerShipMemory = allocateOwnerShipMemory(alignedSize);
                        memoryQueue.offer(ownerShipMemory);
                        return ownerShipMemory.resource();
                    } finally {
                        reentrantReadWriteLock.readLock().unlock();
                    }
                } else {
                    throw new IllegalStateException("arena is closed");
                }
            }

            @Override
            public MemorySegment.Scope scope() {
                return Arena.global().scope();
            }

            @Override
            public void close() {
                reentrantReadWriteLock.writeLock().lock();
                try {
                    while (!memoryQueue.isEmpty()) {
                        memoryQueue.poll().drop();
                    }
                } finally {
                    reentrantReadWriteLock.writeLock().unlock();
                }
            }
        };
    }

    MemoryAllocator<OwnershipMemory> LIBC_MALLOC = size -> {
        var memorySegment = Instance.LIBC_MALLOC.malloc(size);
        return new OwnershipMemory() {

            private final CloseHandle freeHandle = new CloseHandle(() -> Instance.LIBC_MALLOC.free(memorySegment));

            @Override
            public MemorySegment resource() {
                return memorySegment;
            }

            @Override
            public void drop() {
                freeHandle.close();
            }
        };
    };
}
