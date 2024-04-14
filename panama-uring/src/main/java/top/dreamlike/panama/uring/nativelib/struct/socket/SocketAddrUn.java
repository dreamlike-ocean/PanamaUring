package top.dreamlike.panama.uring.nativelib.struct.socket;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class SocketAddrUn {

    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(SocketAddrUn.class);
    public static final long SUN_PATH_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("sun_path"));


    private short sun_family;

    @NativeArrayMark(length = 108, size = byte.class)
    private MemorySegment sun_path;

    public short getSun_family() {
        return sun_family;
    }

    public void setSun_family(short sun_family) {
        this.sun_family = sun_family;
    }

    public MemorySegment getSun_path() {
        return sun_path;
    }

    public void setSun_path(MemorySegment sun_path) {
        this.sun_path = sun_path;
    }
}
