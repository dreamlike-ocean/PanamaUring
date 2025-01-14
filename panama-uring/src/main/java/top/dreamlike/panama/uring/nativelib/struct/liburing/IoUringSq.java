package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class IoUringSq {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSq.class);

    @Pointer(targetLayout = int.class)
    private MemorySegment khead;
    @Pointer(targetLayout = int.class)
    private MemorySegment ktail;
    @Pointer
    private MemorySegment kring_mask;
    @Pointer
    private MemorySegment kring_entries;
    @Pointer(targetLayout = int.class)
    private MemorySegment kflags;
    @Pointer
    private MemorySegment kdropped;
    @Pointer
    private MemorySegment array;
    private NativeArrayPointer<IoUringSqe> sqes;
    private int sqe_head;
    private int sqe_tail;
    private long ring_sz;
    @Pointer
    private MemorySegment ring_ptr;

    private int ring_mask;
    private int ring_entries;
    @NativeArrayMark(size = int.class, length = 2)
    private MemorySegment pad;

    public MemorySegment getKhead() {
        return khead;
    }

    public void setKhead(MemorySegment khead) {
        this.khead = khead;
    }

    public MemorySegment getKtail() {
        return ktail;
    }

    public void setKtail(MemorySegment ktail) {
        this.ktail = ktail;
    }

    public MemorySegment getKring_mask() {
        return kring_mask;
    }

    public void setKring_mask(MemorySegment kring_mask) {
        this.kring_mask = kring_mask;
    }

    public MemorySegment getKring_entries() {
        return kring_entries;
    }

    public void setKring_entries(MemorySegment kring_entries) {
        this.kring_entries = kring_entries;
    }

    public MemorySegment getKflags() {
        return kflags;
    }

    public void setKflags(MemorySegment kflags) {
        this.kflags = kflags;
    }

    public MemorySegment getKdropped() {
        return kdropped;
    }

    public void setKdropped(MemorySegment kdropped) {
        this.kdropped = kdropped;
    }

    public MemorySegment getArray() {
        return array;
    }

    public void setArray(MemorySegment array) {
        this.array = array;
    }

    public NativeArrayPointer<IoUringSqe> getSqes() {
        return sqes;
    }

    public void setSqes(NativeArrayPointer<IoUringSqe> sqes) {
        this.sqes = sqes;
    }

    public int getSqe_head() {
        return sqe_head;
    }

    public void setSqe_head(int sqe_head) {
        this.sqe_head = sqe_head;
    }

    public int getSqe_tail() {
        return sqe_tail;
    }

    public void setSqe_tail(int sqe_tail) {
        this.sqe_tail = sqe_tail;
    }

    public long getRing_sz() {
        return ring_sz;
    }

    public void setRing_sz(long ring_sz) {
        this.ring_sz = ring_sz;
    }

    public MemorySegment getRing_ptr() {
        return ring_ptr;
    }

    public void setRing_ptr(MemorySegment ring_ptr) {
        this.ring_ptr = ring_ptr;
    }

    public int getRing_mask() {
        return ring_mask;
    }

    public void setRing_mask(int ring_mask) {
        this.ring_mask = ring_mask;
    }

    public int getRing_entries() {
        return ring_entries;
    }

    public void setRing_entries(int ring_entries) {
        this.ring_entries = ring_entries;
    }

    public MemorySegment getPad() {
        return pad;
    }

    public void setPad(MemorySegment pad) {
        this.pad = pad;
    }
}
