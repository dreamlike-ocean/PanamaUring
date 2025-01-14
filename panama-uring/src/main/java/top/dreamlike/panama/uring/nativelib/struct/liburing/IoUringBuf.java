package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;

public class IoUringBuf {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringBuf.class);

    private long addr;
    private int len;
    private short bid;
    private short resv;

    public long getAddr() {
        return addr;
    }

    public void setAddr(long addr) {
        this.addr = addr;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public short getBid() {
        return bid;
    }

    public void setBid(short bid) {
        this.bid = bid;
    }

    public short getResv() {
        return resv;
    }

    public void setResv(short resv) {
        this.resv = resv;
    }
}
