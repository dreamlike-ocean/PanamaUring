package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.invoke.VarHandle;

public class IoUringRsrcUpdate {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringRsrcUpdate.class);

    public static final VarHandle OFFSET_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("offset"));

    public static final VarHandle DATA_VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("data"));

    private int offset;

    private int resv;

    private long data;

}
