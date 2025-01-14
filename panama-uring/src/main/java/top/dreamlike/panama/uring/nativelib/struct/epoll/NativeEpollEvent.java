package top.dreamlike.panama.uring.nativelib.struct.epoll;

import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.invoke.VarHandle;


public class NativeEpollEvent {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(NativeEpollEvent.class);

    public static final VarHandle U64_VH = LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("data"), MemoryLayout.PathElement.groupElement("u64"))
            .withInvokeExactBehavior();

    public static final VarHandle EVENTS_VH = LAYOUT
            .varHandle(MemoryLayout.PathElement.groupElement("events"))
            .withInvokeExactBehavior();

    private int events;
    private EpollData data;

    public int getEvents() {
        return events;
    }

    public void setEvents(int events) {
        this.events = events;
    }

    public EpollData getData() {
        return data;
    }

    public void setData(EpollData data) {
        this.data = data;
    }

    public long getU64() {
        return (long) U64_VH.get(StructProxyGenerator.findMemorySegment(this), 0L);
    }

    public void setU64(long u64) {
        U64_VH.set(StructProxyGenerator.findMemorySegment(this), 0L, u64);
    }

}
