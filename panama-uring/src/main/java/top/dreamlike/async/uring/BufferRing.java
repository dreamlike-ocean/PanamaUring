package top.dreamlike.async.uring;

import top.dreamlike.nativeLib.liburing.io_uring_buf;
import top.dreamlike.nativeLib.liburing.io_uring_buf_ring;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

public class BufferRing {
    private final MemorySegment buf_ring;
    private final MemorySegment[] buffers;
    private final int mask;
    MemorySession allocator;

    /**
     * Create a new buffer ring with the given capacity.
     *
     * @param capacity the capacity of the ring, will be rounded up to the next
     *                 power of two
     */
    public BufferRing(int capacity) {
        capacity = roundUpToPowerOfTwo(capacity);
        allocator = MemorySession.openShared();
        buf_ring = allocator.allocate(4096, 4096);
        buffers = new MemorySegment[capacity];
        mask = capacity - 1;
        init();
    }

    private static int roundUpToPowerOfTwo(int capacity) {
        return 1 << (32 - Integer.numberOfLeadingZeros(capacity - 1));
    }

    public long unsafeAddress() {
        return buf_ring.address().toRawLongValue();
    }

    private void init() {
        io_uring_buf_ring.tail$set(buf_ring, (short) 0);
    }

    public void add(MemorySegment buffer, short bid, int buf_offset) {
        var idx = (io_uring_buf_ring.tail$get(buf_ring) + buf_offset) & mask;
        var buf = buf_ring.asSlice(idx * 16);
        io_uring_buf.addr$set(buf, buffer.address().toRawLongValue());
        io_uring_buf.len$set(buf, (int) buffer.byteSize());
        io_uring_buf.bid$set(buf, bid);

        buffers[idx] = buffer;
    }

    public void advance(int count) {
        var new_tail = (short) (io_uring_buf_ring.tail$get(buf_ring) + count);
        io_uring_buf_ring.tail$VH().setRelease(buf_ring, new_tail);
    }

    public MemorySegment getBuffer(int idx) {
        return buffers[idx];
    }
}
