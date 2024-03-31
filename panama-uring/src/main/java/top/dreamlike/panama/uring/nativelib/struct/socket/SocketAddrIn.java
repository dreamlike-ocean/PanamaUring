package top.dreamlike.panama.uring.nativelib.struct.socket;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class SocketAddrIn {
    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(SocketAddrIn.class);

    private short sin_family;
    private short sin_port;
    private int sin_addr;

    @NativeArrayMark(size = byte.class, length = 8)
    private MemorySegment sin_zero;

    public short getSin_family() {
        return sin_family;
    }

    public void setSin_family(short sin_family) {
        this.sin_family = sin_family;
    }

    public short getSin_port() {
        return sin_port;
    }

    public void setSin_port(short sin_port) {
        this.sin_port = sin_port;
    }

    public int getSin_addr() {
        return sin_addr;
    }

    public void setSin_addr(int sin_addr) {
        this.sin_addr = sin_addr;
    }

}
