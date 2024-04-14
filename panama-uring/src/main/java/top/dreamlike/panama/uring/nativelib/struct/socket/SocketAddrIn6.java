package top.dreamlike.panama.uring.nativelib.struct.socket;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Union;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class SocketAddrIn6 {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(SocketAddrIn6.class);
    public static final long SIZE = LAYOUT.byteSize();
    public static final long SIN6_ADDR_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("sin6_addr"));
    public static final long SIN6_ADDR_SIZE = Instance.STRUCT_PROXY_GENERATOR.extract(In6Addr.class).byteSize();

    private short sin6_family;
    private short sin6_port;
    private int sin_flowinfo;
    private In6Addr sin6_addr;
    private int sin6_scope_id;

    public short getSin6_family() {
        return sin6_family;
    }

    public void setSin6_family(short sin6_family) {
        this.sin6_family = sin6_family;
    }

    public short getSin6_port() {
        return sin6_port;
    }

    public void setSin6_port(short sin6_port) {
        this.sin6_port = sin6_port;
    }

    public int getSin_flowinfo() {
        return sin_flowinfo;
    }

    public void setSin_flowinfo(int sin_flowinfo) {
        this.sin_flowinfo = sin_flowinfo;
    }

    public In6Addr getSin6_addr() {
        return sin6_addr;
    }

    public void setSin6_addr(In6Addr sin6_addr) {
        this.sin6_addr = sin6_addr;
    }

    public int getSin6_scope_id() {
        return sin6_scope_id;
    }

    public void setSin6_scope_id(int sin6_scope_id) {
        this.sin6_scope_id = sin6_scope_id;
    }

    @Union
    public static class In6Addr {
        @NativeArrayMark(length = 16, size = byte.class)
        private MemorySegment __u6_addr8;
        @NativeArrayMark(length = 8, size = Short.class)
        private MemorySegment __u6_addr16;
        @NativeArrayMark(length = 4, size = int.class)
        private MemorySegment  __u6_addr32;
    }
}