package top.dreamlike.helper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.ref.Cleaner;

public class StackValue implements SegmentAllocator, AutoCloseable {

    private static final long Frame_Size = Long.getLong("StackValue.frameSize", 2048);

    private static final ThreadLocal<StackValue> threadStack = new ThreadLocal<>();
    private static final Cleaner CLEANER = Cleaner.create();
    private MemorySegment stack;
    private Thread owner;
    private int hasAllocate = 0;
    private Arena threadLocalArena;

    private StackValue(MemorySegment stack, Arena threadLocalArena) {
        this.owner = Thread.currentThread();
        this.stack = stack;
        this.threadLocalArena = threadLocalArena;
        CLEANER.register(this, threadLocalArena::close);
    }

    public static StackValue currentStack() {
        StackValue stackValue = threadStack.get();
        if (stackValue != null) {
            return stackValue;
        }
        Arena arena = Arena.ofConfined();
        StackValue value = new StackValue(arena.allocate(Frame_Size), arena);
        threadStack.set(value);
        return value;
    }

    public static void release() {
        StackValue value = threadStack.get();
        if (value == null) {
            return;
        }
        value.stack = null;
        value.threadLocalArena.close();
        threadStack.remove();
    }

    private long alignUp(long base, long byteAlignment) {
        return (base + byteAlignment - 1) & -byteAlignment;
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        assertOwner();
        assertAlign(byteSize, byteAlignment);
        var base = stack.address();
        var start = alignUp(base + hasAllocate, byteAlignment) - base;
        MemorySegment res = stack.asSlice(start, byteSize, byteAlignment);
        hasAllocate += (int) (start + byteSize);
        return res;
    }

    @Override
    public void close() throws Exception {
        assertOwner();
        hasAllocate = 0;
    }

    public void reset() {
        assertOwner();
        hasAllocate = 0;
    }

    private void assertOwner() {
        if (Thread.currentThread() != owner) {
            throw new WrongThreadException("Not on the thread of the stack");
        }
    }

    private void assertAlign(long byteSize, long byteAlignment) {
        if (byteSize < 0) {
            throw new IllegalArgumentException("Invalid allocation size : " + byteSize);
        }

        // alignment should be > 0, and power of two
        if (byteAlignment <= 0 || ((byteAlignment & (byteAlignment - 1)) != 0L)) {
            throw new IllegalArgumentException("Invalid alignment constraint : " + byteAlignment);
        }
    }
}
