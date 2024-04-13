package top.dreamlike.panama.uring.nativelib.struct.inotify;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

public class NativeInotifyEvent {
    public static final MemoryLayout LAYOUT = Instance.STRUCT_PROXY_GENERATOR.extract(NativeInotifyEvent.class);

    public static final VarHandle WD$VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("wd"));
    public static final VarHandle MASK$VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("mask"));
    public static final VarHandle COOKIE$VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cookie"));
    public static final VarHandle LEN$VH = LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("len"));
    public static final long NAME_OFFSET = LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("name"));


    private int wd;
    private int mask;
    private int cookie;
    private int len;

    @NativeArrayMark(length = 0, size = byte.class)
    private MemorySegment name;


    public int getWd() {
        return wd;
    }

    public void setWd(int wd) {
        this.wd = wd;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getCookie() {
        return cookie;
    }

    public void setCookie(int cookie) {
        this.cookie = cookie;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public MemorySegment getName() {
        return name;
    }

    public void setName(MemorySegment name) {
        this.name = name;
    }
}
