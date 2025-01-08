package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

public class IoUringBufStatus {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringBufStatus.class);

    public static final VarHandle BUF_GROUP_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bufGroup"));

    public static final VarHandle HEADER_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("head"));

    private int bufGroup;

    private int head;

    @NativeArrayMark(size = int.class, length = 8)
    private MemorySegment recv;

    public int getBufGroup() {
        return bufGroup;
    }

    public void setBufGroup(int bufGroup) {
        this.bufGroup = bufGroup;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public MemorySegment getRecv() {
        return recv;
    }

    public void setRecv(MemorySegment recv) {
        this.recv = recv;
    }
}
