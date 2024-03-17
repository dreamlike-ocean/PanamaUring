package top.dreamlike.panama.nativelib.struct.socket;

import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.nativelib.struct.iovec.Iovec;

import java.lang.foreign.MemorySegment;

public class MsgHdr {
    @Pointer
    private MemorySegment msg_name;

    private int msg_namelen;

    @Pointer
    private Iovec msg_iov;
    private long msg_iovlen;

    @Pointer
    private MemorySegment msg_control;
    private long msg_controllen;
    private int msg_flags;

    public MemorySegment getMsg_name() {
        return msg_name;
    }

    public void setMsg_name(MemorySegment msg_name) {
        this.msg_name = msg_name;
    }

    public int getMsg_namelen() {
        return msg_namelen;
    }

    public void setMsg_namelen(int msg_namelen) {
        this.msg_namelen = msg_namelen;
    }

    public Iovec getMsg_iov() {
        return msg_iov;
    }

    public void setMsg_iov(Iovec msg_iov) {
        this.msg_iov = msg_iov;
    }

    public long getMsg_iovlen() {
        return msg_iovlen;
    }

    public void setMsg_iovlen(long msg_iovlen) {
        this.msg_iovlen = msg_iovlen;
    }

    public MemorySegment getMsg_control() {
        return msg_control;
    }

    public void setMsg_control(MemorySegment msg_control) {
        this.msg_control = msg_control;
    }

    public long getMsg_controllen() {
        return msg_controllen;
    }

    public void setMsg_controllen(long msg_controllen) {
        this.msg_controllen = msg_controllen;
    }

    public int getMsg_flags() {
        return msg_flags;
    }

    public void setMsg_flags(int msg_flags) {
        this.msg_flags = msg_flags;
    }
}
