package top.dreamlike.panama.nativelib.struct.socket;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

public class SocketAddrIn {
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
