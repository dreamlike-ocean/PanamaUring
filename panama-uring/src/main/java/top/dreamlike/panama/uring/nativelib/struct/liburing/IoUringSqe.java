package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.Alignment;
import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Union;
import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

public class IoUringSqe {
    private byte opcode;
    private byte flags;
    private short ioprio;
    private int fd;

    private OffsetUnion offsetUnion;
    private BufferUnion bufferUnion;
    private int len;
    private FlagsUnion flagsUnion;
    private long user_data;
    private FixeBufferUnion fixeBuffer;
    private short personality;
    private SpliceUnion spliceUnion;

    private Addr3Union addr3Union;

    public boolean isLinked() {
        return (getFlags() & IoUringConstant.IOSQE_IO_LINK) != 0;
    }

    public OffsetUnion getOffsetUnion() {
        return offsetUnion;
    }

    public void setOffsetUnion(OffsetUnion offsetUnion) {
        this.offsetUnion = offsetUnion;
    }

    public byte getOpcode() {
        return opcode;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public short getIoprio() {
        return ioprio;
    }

    public void setIoprio(short ioprio) {
        this.ioprio = ioprio;
    }

    public int getFd() {
        return fd;
    }

    public void setFd(int fd) {
        this.fd = fd;
    }


    public BufferUnion getBufferUnion() {
        return bufferUnion;
    }

    public void setBufferUnion(BufferUnion bufferUnion) {
        this.bufferUnion = bufferUnion;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public FlagsUnion getFlagsUnion() {
        return flagsUnion;
    }

    public void setFlagsInFlagsUnion(FlagsUnion flagsUnion) {
        this.flagsUnion = flagsUnion;
    }

    public long getUser_data() {
        return user_data;
    }

    public void setUser_data(long user_data) {
        this.user_data = user_data;
    }

    public FixeBufferUnion getFixeBuffer() {
        return fixeBuffer;
    }

    public void setFixeBuffer(FixeBufferUnion fixeBuffer) {
        this.fixeBuffer = fixeBuffer;
    }

    public short getPersonality() {
        return personality;
    }

    public void setPersonality(short personality) {
        this.personality = personality;
    }

    public SpliceUnion getSpliceUnion() {
        return spliceUnion;
    }

    public void setSpliceUnion(SpliceUnion spliceUnion) {
        this.spliceUnion = spliceUnion;
    }

    public Addr3Union getAddr3Union() {
        return addr3Union;
    }

    public void setAddr3Union(Addr3Union addr3Union) {
        this.addr3Union = addr3Union;
    }

    public void setOffset(long offset) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IOURING_SQE_OFFSET_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, offset);
    }

    public long getOffset() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IOURING_SQE_OFFSET_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (long) varHandle.get(memory, 0L);
    }

    public void setAddr2(long addr2) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IOURING_SQE_ADDR2_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, addr2);
    }

    public long getAddr2() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IOURING_SQE_ADDR2_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (long) varHandle.get(memory, 0L);
    }

    public void setAddr(long addr) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_ADDR_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, addr);
    }

    public long getAddr() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_ADDR_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (long) varHandle.get(memory, 0L);
    }

    public void setFlagsInFlagsUnion(int rwFlags) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_RW_FLAGS_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, rwFlags);
    }

    public int getRwFlags() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_RW_FLAGS_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (int) varHandle.get(memory, 0L);
    }

    public short getBufIndex() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_BUF_INDEX_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (short) varHandle.get(memory, 0L);
    }

    public void setBufIndex(short bufIndex) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_BUF_INDEX_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, bufIndex);
    }

    public short getBufGroup() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_BUF_GROUP_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (short) varHandle.get(memory, 0L);
    }

    public void setBufGroup(short bufGroup) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_BUF_GROUP_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, bufGroup);
    }

    public int getFileIndex() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_FILE_INDEX_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (int) varHandle.get(memory, 0L);
    }

    public void setFileIndex(int fileIndex) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_FILE_INDEX_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, fileIndex);
    }

    public void setAddr3(long addr3) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_ADDR3_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, addr3);
    }

    public long getAddr3() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SQE_ADDR3_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (long) varHandle.get(memory, 0L);
    }

    public void setPad2(long pad2) {
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        memory.set(ValueLayout.JAVA_LONG, IoUringConstant.AccessShortcuts.IO_URING_SQE__PAD2_OFFSET, pad2);
    }

    public long getPad2() {
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return memory.get(ValueLayout.JAVA_LONG, IoUringConstant.AccessShortcuts.IO_URING_SQE__PAD2_OFFSET);
    }

    public long getSpliceOffIn() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_OFF_IN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (long) varHandle.get(memory, 0L);
    }

    public void setSpliceOffIn(long spliceOffIn) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_OFF_IN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, spliceOffIn);
    }

    public int getSpliceFdIn() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_FD_IN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (int) varHandle.get(memory, 0L);
    }

    public void setSpliceFdIn(int spliceFdIn) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_FD_IN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, spliceFdIn);
    }

    public int getSpliceFlags() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_FD_OUT_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (int) varHandle.get(memory, 0L);
    }

    public void setSpliceFlags(int spliceFlags) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_SPLICE_FD_OUT_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, spliceFlags);
    }

    public void setAddrLen(short addrLen) {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_ADDR_LEN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        varHandle.set(memory, 0L, addrLen);
    }

    public short getAddrLen() {
        VarHandle varHandle = IoUringConstant.AccessShortcuts.IO_URING_ADDR_LEN_VARHANDLE;
        MemorySegment memory = ((NativeStructEnhanceMark) this).realMemory();
        return (short) varHandle.get(memory, 0L);
    }


    @Union
    public static class OffsetUnion {
        private long off;
        private long addr2;

        private CmdOp cmdOp;

        public long getOff() {
            return off;
        }

        public void setOff(long off) {
            this.off = off;
        }

        public long getAddr2() {
            return addr2;
        }

        public void setAddr2(long addr2) {
            this.addr2 = addr2;
        }

        public CmdOp getCmdOp() {
            return cmdOp;
        }

        public void setCmdOp(CmdOp cmdOp) {
            this.cmdOp = cmdOp;
        }

        public static class CmdOp {
            private int cmd_op;
            private int __pad1;

            public int getCmd_op() {
                return cmd_op;
            }

            public void setCmd_op(int cmd_op) {
                this.cmd_op = cmd_op;
            }

            public int get__pad1() {
                return __pad1;
            }

            public void set__pad1(int __pad1) {
                this.__pad1 = __pad1;
            }
        }
    }

    @Union
    public static class BufferUnion {
        private long addr;
        private long splice_off_in;
        private Level level;

        public long getAddr() {
            return addr;
        }

        public void setAddr(long addr) {
            this.addr = addr;
        }

        public long getSplice_off_in() {
            return splice_off_in;
        }

        public void setSplice_off_in(long splice_off_in) {
            this.splice_off_in = splice_off_in;
        }

        public Level getLevel() {
            return level;
        }

        public void setLevel(Level level) {
            this.level = level;
        }

        public static class Level {
            private int level;
            private int optname;

            public int getLevel() {
                return level;
            }

            public void setLevel(int level) {
                this.level = level;
            }

            public int getOptname() {
                return optname;
            }

            public void setOptname(int optname) {
                this.optname = optname;
            }
        }
    }

    @Union
    public static class FlagsUnion {
        private int rw_flags;
        private int fsync_flags;
        private short poll_events;
        private int poll32_events;
        private int sync_range_flags;
        private int msg_flags;
        private int timeout_flags;
        private int accept_flags;
        private int cancel_flags;
        private int open_flags;
        private int statx_flags;
        private int fadvise_advice;
        private int splice_flags;
        private int rename_flags;
        private int unlink_flags;
        private int hardlink_flags;
        private int xattr_flags;
        private int msg_ring_flags;
        private int uring_cmd_flags;
        private int waitid_flags;
        private int futex_flags;
        private int install_fd_flags;

        public int getRw_flags() {
            return rw_flags;
        }

        public void setRw_flags(int rw_flags) {
            this.rw_flags = rw_flags;
        }

        public int getFsync_flags() {
            return fsync_flags;
        }

        public void setFsync_flags(int fsync_flags) {
            this.fsync_flags = fsync_flags;
        }

        public short getPoll_events() {
            return poll_events;
        }

        public void setPoll_events(short poll_events) {
            this.poll_events = poll_events;
        }

        public int getHardlink_flags() {
            return hardlink_flags;
        }

        public void setHardlink_flags(int hardlink_flags) {
            this.hardlink_flags = hardlink_flags;
        }

        public int getXattr_flags() {
            return xattr_flags;
        }

        public void setXattr_flags(int xattr_flags) {
            this.xattr_flags = xattr_flags;
        }

        public int getMsg_ring_flags() {
            return msg_ring_flags;
        }

        public void setMsg_ring_flags(int msg_ring_flags) {
            this.msg_ring_flags = msg_ring_flags;
        }

        public int getUring_cmd_flags() {
            return uring_cmd_flags;
        }

        public void setUring_cmd_flags(int uring_cmd_flags) {
            this.uring_cmd_flags = uring_cmd_flags;
        }

        public int getWaitid_flags() {
            return waitid_flags;
        }

        public void setWaitid_flags(int waitid_flags) {
            this.waitid_flags = waitid_flags;
        }

        public int getFutex_flags() {
            return futex_flags;
        }

        public void setFutex_flags(int futex_flags) {
            this.futex_flags = futex_flags;
        }

        public int getInstall_fd_flags() {
            return install_fd_flags;
        }

        public void setInstall_fd_flags(int install_fd_flags) {
            this.install_fd_flags = install_fd_flags;
        }

        public int getPoll32_events() {
            return poll32_events;
        }

        public void setPoll32_events(int poll32_events) {
            this.poll32_events = poll32_events;
        }

        public int getSync_range_flags() {
            return sync_range_flags;
        }

        public void setSync_range_flags(int sync_range_flags) {
            this.sync_range_flags = sync_range_flags;
        }

        public int getMsg_flags() {
            return msg_flags;
        }

        public void setMsg_flags(int msg_flags) {
            this.msg_flags = msg_flags;
        }

        public int getTimeout_flags() {
            return timeout_flags;
        }

        public void setTimeout_flags(int timeout_flags) {
            this.timeout_flags = timeout_flags;
        }

        public int getAccept_flags() {
            return accept_flags;
        }

        public void setAccept_flags(int accept_flags) {
            this.accept_flags = accept_flags;
        }

        public int getCancel_flags() {
            return cancel_flags;
        }

        public void setCancel_flags(int cancel_flags) {
            this.cancel_flags = cancel_flags;
        }

        public int getOpen_flags() {
            return open_flags;
        }

        public void setOpen_flags(int open_flags) {
            this.open_flags = open_flags;
        }

        public int getStatx_flags() {
            return statx_flags;
        }

        public void setStatx_flags(int statx_flags) {
            this.statx_flags = statx_flags;
        }

        public int getFadvise_advice() {
            return fadvise_advice;
        }

        public void setFadvise_advice(int fadvise_advice) {
            this.fadvise_advice = fadvise_advice;
        }

        public int getSplice_flags() {
            return splice_flags;
        }

        public void setSplice_flags(int splice_flags) {
            this.splice_flags = splice_flags;
        }

        public int getRename_flags() {
            return rename_flags;
        }

        public void setRename_flags(int rename_flags) {
            this.rename_flags = rename_flags;
        }

        public int getUnlink_flags() {
            return unlink_flags;
        }

        public void setUnlink_flags(int unlink_flags) {
            this.unlink_flags = unlink_flags;
        }
    }

    @Union
    @Alignment(byteSize = 1)
    public static class FixeBufferUnion {
        private short buf_index;
        private short buf_group;

        public short getBuf_index() {
            return buf_index;
        }

        public void setBuf_index(short buf_index) {
            this.buf_index = buf_index;
        }

        public short getBuf_group() {
            return buf_group;
        }

        public void setBuf_group(short buf_group) {
            this.buf_group = buf_group;
        }
    }

    @Union
    public static class SpliceUnion {
        private int splice_fd_in;
        private int file_index;
        private int optlen;

        private AddrLen len;

        public int getSplice_fd_in() {
            return splice_fd_in;
        }

        public void setSplice_fd_in(int splice_fd_in) {
            this.splice_fd_in = splice_fd_in;
        }

        public int getFile_index() {
            return file_index;
        }

        public void setFile_index(int file_index) {
            this.file_index = file_index;
        }

        public int getOptlen() {
            return optlen;
        }

        public void setOptlen(int optlen) {
            this.optlen = optlen;
        }

        public AddrLen getLen() {
            return len;
        }

        public void setLen(AddrLen len) {
            this.len = len;
        }

        public static class AddrLen {
            private short addr_len;
            @NativeArrayMark(size = short.class, length = 1)
            private MemorySegment __pad3;

            public short getAddr_len() {
                return addr_len;
            }

            public void setAddr_len(short addr_len) {
                this.addr_len = addr_len;
            }

            public MemorySegment get__pad3() {
                return __pad3;
            }

            public void set__pad3(MemorySegment __pad3) {
                this.__pad3 = __pad3;
            }
        }
    }

    @Union
    public static class Addr3Union {
        private Addr3 addr3;

        private long optval;

        @NativeArrayMark(size = byte.class, length = 0)
        private MemorySegment cmd;

        public Addr3 getAddr3() {
            return addr3;
        }

        public void setAddr3(Addr3 addr3) {
            this.addr3 = addr3;
        }

        public long getOptval() {
            return optval;
        }

        public void setOptval(long optval) {
            this.optval = optval;
        }

        public MemorySegment getCmd() {
            return cmd;
        }

        public void setCmd(MemorySegment cmd) {
            this.cmd = cmd;
        }


        public static class Addr3 {
            private long addr3;
            @NativeArrayMark(size = long.class, length = 1)
            private MemorySegment __pad2;

            public long getAddr3() {
                return addr3;
            }

            public void setAddr3(long addr3) {
                this.addr3 = addr3;
            }

            public MemorySegment get__pad2() {
                return __pad2;
            }

            public void set__pad2(MemorySegment __pad2) {
                this.__pad2 = __pad2;
            }
        }
    }

}
