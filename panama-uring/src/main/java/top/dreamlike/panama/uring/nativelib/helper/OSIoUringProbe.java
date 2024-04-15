package top.dreamlike.panama.uring.nativelib.helper;

import top.dreamlike.panama.generator.proxy.NativeArray;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemorySegment;

public class OSIoUringProbe {

    private final int lastOp;

    private final IoUringProbeOp[] ops;

    public OSIoUringProbe() {
        var probe = Instance.LIB_URING.io_uring_get_probe();
        if (probe == null) {
            throw new RuntimeException("Failed to get probe");
        }
        lastOp = probe.getLastOp();
        byte len = probe.getOpsLen();
        ops = new IoUringProbeOp[len];
        MemorySegment opsBase = StructProxyGenerator.findMemorySegment(probe)
                .asSlice(top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringProbe.OPS_OFFSET)
                .reinterpret(len * top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringProbeOp.LAYOUT.byteSize());

        NativeArray<top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringProbeOp> ops = Instance.STRUCT_PROXY_GENERATOR.enhanceArray(opsBase);
        for (byte i = 0; i < len; i++) {
            top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringProbeOp op = ops.get(i);
            this.ops[i] = new IoUringProbeOp(op.getOp(), op.getFlags());
        }

        Instance.LIB_URING.io_uring_free_probe(probe);
    }

    public int getLastOp() {
        return lastOp;
    }

    public IoUringProbeOp[] getOps() {
        return ops;
    }

    public record IoUringProbeOp(byte op, short flags) {
    }
}
