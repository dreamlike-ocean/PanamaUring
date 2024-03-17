package top.dreamlike.panama.nativelib.struct.liburing;

import top.dreamlike.panama.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.invoke.VarHandle;

public class IoUringConstant {
    public static class Opcode {
        public static final int IORING_OP_NOP = 0;
        public static final int IORING_OP_READV = 1;
        public static final int IORING_OP_WRITEV = 2;
        public static final int IORING_OP_FSYNC = 3;
        public static final int IORING_OP_READ_FIXED = 4;
        public static final int IORING_OP_WRITE_FIXED = 5;
        public static final int IORING_OP_POLL_ADD = 6;
        public static final int IORING_OP_POLL_REMOVE = 7;
        public static final int IORING_OP_SYNC_FILE_RANGE = 8;
        public static final int IORING_OP_SENDMSG = 9;
        public static final int IORING_OP_RECVMSG = 10;
        public static final int IORING_OP_TIMEOUT = 11;
        public static final int IORING_OP_TIMEOUT_REMOVE = 12;
        public static final int IORING_OP_ACCEPT = 13;
        public static final int IORING_OP_ASYNC_CANCEL = 14;
        public static final int IORING_OP_LINK_TIMEOUT = 15;
        public static final int IORING_OP_CONNECT = 16;
        public static final int IORING_OP_FALLOCATE = 17;
        public static final int IORING_OP_OPENAT = 18;
        public static final int IORING_OP_CLOSE = 19;
        public static final int IORING_OP_FILES_UPDATE = 20;
        public static final int IORING_OP_STATX = 21;
        public static final int IORING_OP_READ = 22;
        public static final int IORING_OP_WRITE = 23;
        public static final int IORING_OP_FADVISE = 24;
        public static final int IORING_OP_MADVISE = 25;
        public static final int IORING_OP_SEND = 26;
        public static final int IORING_OP_RECV = 27;
        public static int IORING_OP_OPENAT2 = 28;
        public static int IORING_OP_EPOLL_CTL = 29;
        public static int IORING_OP_SPLICE = 30;
        public static int IORING_OP_PROVIDE_BUFFERS = 31;
        public static int IORING_OP_REMOVE_BUFFERS = 32;
        public static int IORING_OP_TEE = 33;
        public static int IORING_OP_SHUTDOWN = 34;
        public static int IORING_OP_RENAMEAT = 35;
        public static int IORING_OP_UNLINKAT = 36;
        public static int IORING_OP_MKDIRAT = 37;
        public static int IORING_OP_SYMLINKAT = 38;
        public static int IORING_OP_LINKAT = 39;
        public static int IORING_OP_MSG_RING = 40;
        public static int IORING_OP_FSETXATTR = 41;
        public static int IORING_OP_SETXATTR = 42;
        public static int IORING_OP_FGETXATTR = 43;
        public static int IORING_OP_GETXATTR = 44;
        public static int IORING_OP_SOCKET = 45;
        public static int IORING_OP_URING_CMD = 46;
        public static int IORING_OP_SEND_ZC = 47;
        public static int IORING_OP_SENDMSG_ZC = 48;
        public static int IORING_OP_READ_MULTISHOT = 49;
        public static int IORING_OP_WAITID = 50;
        public static int IORING_OP_FUTEX_WAIT = 51;
        public static int IORING_OP_FUTEX_WAKE = 52;
        public static int IORING_OP_FUTEX_WAITV = 53;
        public static int IORING_OP_FIXED_FD_INSTALL = 54;
        public static int IORING_OP_FTRUNCATE = 55;

    }

    public static class AccessShortcuts {
        public static final MemoryLayout IoUringSqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class);
        public static final VarHandle IOURING_SQE_OFFSET_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("offsetUnion"),
                MemoryLayout.PathElement.groupElement("off")
        );
        public static VarHandle IO_URING_SQE_ADDR_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("bufferUnion"),
                MemoryLayout.PathElement.groupElement("addr")
        );
        public static VarHandle IO_URING_SQE_RW_FLAGS_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("flagsUnion"),
                MemoryLayout.PathElement.groupElement("rw_flags")
        );
        public static VarHandle IO_URING_SQE_BUF_INDEX_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("fixeBuffer"),
                MemoryLayout.PathElement.groupElement("buf_index")
        );
        public static VarHandle IO_URING_SQE_FILE_INDEX_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("spliceUnion"),
                MemoryLayout.PathElement.groupElement("file_index")
        );
        public static VarHandle IO_URING_SQE_ADDR3_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("addr3Union"),
                MemoryLayout.PathElement.groupElement("addr3"),
                MemoryLayout.PathElement.groupElement("addr3")
        );

        public static long IO_URING_SQE__PAD2_OFFSET = IoUringSqeLayout.byteOffset(
                MemoryLayout.PathElement.groupElement("addr3Union"),
                MemoryLayout.PathElement.groupElement("addr3"),
                MemoryLayout.PathElement.groupElement("__pad2")
        );

        public static VarHandle IO_URING_SPLICE_OFF_IN_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("bufferUnion"),
                MemoryLayout.PathElement.groupElement("splice_off_in")
        );

        public static VarHandle IO_URING_SPLICE_FD_IN_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("spliceUnion"),
                MemoryLayout.PathElement.groupElement("splice_fd_in")
        );

        public static VarHandle IO_URING_SPLICE_FD_OUT_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("flagsUnion"),
                MemoryLayout.PathElement.groupElement("splice_flags")
        );
    }

    /**
     * send/sendmsg and recv/recvmsg flags (sqe->ioprio)
     * IORING_RECVSEND_POLL_FIRST	If set, instead of first attempting to send
     * or receive and arm poll if that yields an
     * -EAGAIN result, arm poll upfront and skip
     * the initial transfer attempt.
     * IORING_RECV_MULTISHOT	Multishot recv. Sets IORING_CQE_F_MORE if
     * the handler will continue to report
     * CQEs on behalf of the same SQE.
     * IORING_RECVSEND_FIXED_BUF	Use registered buffers, the index is stored in
     * the buf_index field.
     * IORING_SEND_ZC_REPORT_USAGE
     * If set, SEND[MSG]_ZC should report
     * the zerocopy usage in cqe.res
     * for the IORING_CQE_F_NOTIF cqe.
     * 0 is reported if zerocopy was actually possible.
     * IORING_NOTIF_USAGE_ZC_COPIED if data was copied
     * (at least partially).
     */
    public static short IORING_RECVSEND_POLL_FIRST = 1 << 0;
    public static short IORING_RECV_MULTISHOT = 1 << 1;
    public static short IORING_RECVSEND_FIXED_BUF = 1 << 2;
    public static short IORING_SEND_ZC_REPORT_USAGE = 1 << 3;

    /**
     * POLL_ADD flags. Note that since sqe->poll_events is the flag space, the
     * command flags for POLL_ADD are stored in sqe->len.
     * IORING_POLL_ADD_MULTI	Multishot poll. Sets IORING_CQE_F_MORE if
     * the poll handler will continue to report
     * CQEs on behalf of the same SQE.
     * <p>
     * IORING_POLL_UPDATE		Update existing poll request, matching
     * sqe->addr as the old user_data field.
     * IORING_POLL_LEVEL		Level triggered poll.
     */
    public static int IORING_POLL_ADD_MULTI = 1 << 0;
    public static int IORING_POLL_UPDATE_EVENTS = 1 << 1;
    public static int IORING_POLL_UPDATE_USER_DATA = 1 << 2;
    public static int IORING_POLL_ADD_LEVEL = 1 << 3;


}
