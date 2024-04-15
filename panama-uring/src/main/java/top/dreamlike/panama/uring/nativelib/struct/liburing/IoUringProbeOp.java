package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;

public class IoUringProbeOp {
    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringProbeOp.class);

    private byte op;

    private byte resv;

    private short flags;

    private int recv2;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        this.op = op;
    }

    public byte getResv() {
        return resv;
    }

    public void setResv(byte resv) {
        this.resv = resv;
    }

    public short getFlags() {
        return flags;
    }

    public void setFlags(short flags) {
        this.flags = flags;
    }

    public int getRecv2() {
        return recv2;
    }

    public void setRecv2(int recv2) {
        this.recv2 = recv2;
    }
}
//struct io_uring_probe_op {
//	__u8 op;
//	__u8 resv;
//	__u16 flags;	/* IO_URING_OP_* flags */
//	__u32 resv2;
//};