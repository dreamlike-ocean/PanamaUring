package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.async.uring.IOUring;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.lang.foreign.MemoryLayout;
import java.lang.invoke.VarHandle;

public class IoUringConstant {
    public static class Opcode {
        public static final byte IORING_OP_NOP = 0;
        public static final byte IORING_OP_READV = 1;
        public static final byte IORING_OP_WRITEV = 2;
        public static final byte IORING_OP_FSYNC = 3;
        public static final byte IORING_OP_READ_FIXED = 4;
        public static final byte IORING_OP_WRITE_FIXED = 5;
        public static final byte IORING_OP_POLL_ADD = 6;
        public static final byte IORING_OP_POLL_REMOVE = 7;
        public static final byte IORING_OP_SYNC_FILE_RANGE = 8;
        public static final byte IORING_OP_SENDMSG = 9;
        public static final byte IORING_OP_RECVMSG = 10;
        public static final byte IORING_OP_TIMEOUT = 11;
        public static final byte IORING_OP_TIMEOUT_REMOVE = 12;
        public static final byte IORING_OP_ACCEPT = 13;
        public static final byte IORING_OP_ASYNC_CANCEL = 14;
        public static final byte IORING_OP_LINK_TIMEOUT = 15;
        public static final byte IORING_OP_CONNECT = 16;
        public static final byte IORING_OP_FALLOCATE = 17;
        public static final byte IORING_OP_OPENAT = 18;
        public static final byte IORING_OP_CLOSE = 19;
        public static final byte IORING_OP_FILES_UPDATE = 20;
        public static final byte IORING_OP_STATX = 21;
        public static final byte IORING_OP_READ = 22;
        public static final byte IORING_OP_WRITE = 23;
        public static final byte IORING_OP_FADVISE = 24;
        public static final byte IORING_OP_MADVISE = 25;
        public static final byte IORING_OP_SEND = 26;
        public static final byte IORING_OP_RECV = 27;
        public static final byte IORING_OP_OPENAT2 = 28;
        public static final byte IORING_OP_EPOLL_CTL = 29;
        public static final byte IORING_OP_SPLICE = 30;
        public static final byte IORING_OP_PROVIDE_BUFFERS = 31;
        public static final byte IORING_OP_REMOVE_BUFFERS = 32;
        public static final byte IORING_OP_TEE = 33;
        public static final byte IORING_OP_SHUTDOWN = 34;
        public static final byte IORING_OP_RENAMEAT = 35;
        public static final byte IORING_OP_UNLINKAT = 36;
        public static final byte IORING_OP_MKDIRAT = 37;
        public static final byte IORING_OP_SYMLINKAT = 38;
        public static final byte IORING_OP_LINKAT = 39;
        public static final byte IORING_OP_MSG_RING = 40;
        public static final byte IORING_OP_FSETXATTR = 41;
        public static final byte IORING_OP_SETXATTR = 42;
        public static final byte IORING_OP_FGETXATTR = 43;
        public static final byte IORING_OP_GETXATTR = 44;
        public static final byte IORING_OP_SOCKET = 45;
        public static final byte IORING_OP_URING_CMD = 46;
        public static final byte IORING_OP_SEND_ZC = 47;
        public static final byte IORING_OP_SENDMSG_ZC = 48;
        public static final byte IORING_OP_READ_MULTISHOT = 49;
        public static final byte IORING_OP_WAITID = 50;
        public static final byte IORING_OP_FUTEX_WAIT = 51;
        public static final byte IORING_OP_FUTEX_WAKE = 52;
        public static final byte IORING_OP_FUTEX_WAITV = 53;
        public static final byte IORING_OP_FIXED_FD_INSTALL = 54;
        public static final byte IORING_OP_FTRUNCATE = 55;

    }

    public static class AccessShortcuts {
        public static final MemoryLayout IoUringSqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class);
        public static final MemoryLayout IoUringCqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCqe.class);
        public static final VarHandle IOURING_SQE_OFFSET_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("offsetUnion"),
                MemoryLayout.PathElement.groupElement("off")
        );

        public static final VarHandle IOURING_SQE_ADDR2_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("offsetUnion"),
                MemoryLayout.PathElement.groupElement("addr2")
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

        public static VarHandle IO_URING_SQE_BUF_GROUP_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("fixeBuffer"),
                MemoryLayout.PathElement.groupElement("buf_group")
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

        public static VarHandle IO_URING_ADDR_LEN_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("spliceUnion"),
                MemoryLayout.PathElement.groupElement("len"),
                MemoryLayout.PathElement.groupElement("addr_len")
        );

        public static VarHandle IO_URING_SPLICE_FD_OUT_VARHANDLE = IoUringSqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("flagsUnion"),
                MemoryLayout.PathElement.groupElement("splice_flags")
        );
        public static final MemoryLayout IoUringLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUring.class);
        public static final VarHandle IO_URING_SQ_KHEAD_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("khead"),
                MemoryLayout.PathElement.dereferenceElement()
        );
        public static final VarHandle IO_URING_SQ_KTAIL_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("ktail"),
                MemoryLayout.PathElement.dereferenceElement()
        );
        public static final VarHandle IO_URING_SQ_SQE_TAIL_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("sqe_tail")
        );
        public static final VarHandle IO_URING_SQ_RING_MASK_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("ring_mask")
        );
        public static final VarHandle IO_URING_SQ_RING_ENTRIES_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("ring_entries")
        );
        public static final VarHandle IO_URING_SQ_SQES_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("sqes")
        );

        public static final VarHandle IO_URING_SQ_KFLAGS_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("sq"),
                MemoryLayout.PathElement.groupElement("kflags"),
                MemoryLayout.PathElement.dereferenceElement()
        );

        public static final VarHandle IO_URING_CQ_KHEAD_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("cq"),
                MemoryLayout.PathElement.groupElement("khead"),
                MemoryLayout.PathElement.dereferenceElement()
        );
        public static final VarHandle IO_URING_CQ_KTAIL_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("cq"),
                MemoryLayout.PathElement.groupElement("ktail"),
                MemoryLayout.PathElement.dereferenceElement()
        );
        public static final VarHandle IO_URING_CQ_KFLAGS_DEFERENCE_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("cq"),
                MemoryLayout.PathElement.groupElement("kflags"),
                MemoryLayout.PathElement.dereferenceElement()
        );
        public static final VarHandle IO_URING_CQ_RING_MASK_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("cq"),
                MemoryLayout.PathElement.groupElement("ring_mask")
        );

        public static final VarHandle IO_URING_CQ_CQES_VARHANDLE = IoUringLayout.varHandle(
                MemoryLayout.PathElement.groupElement("cq"),
                MemoryLayout.PathElement.groupElement("cqes")
        );
    }

    /*
     * If COOP_TASKRUN is set, get notified if task work is available for
     * running and a kernel transition would be needed to run it. This sets
     * IORING_SQ_TASKRUN in the sq ring flags. Not valid with COOP_TASKRUN.
     */
    public static final int IORING_SETUP_TASKRUN_FLAG = 1 << 9;
    public static final int IORING_SETUP_SQE128 = 1 << 10; /* SQEs are 128 byte */
    public static final int IORING_SETUP_CQE32 = 1 << 11; /* CQEs are 32 byte */

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
    public static final short IORING_RECVSEND_POLL_FIRST = 1 << 0;
    public static final short IORING_RECV_MULTISHOT = 1 << 1;
    public static final short IORING_RECVSEND_FIXED_BUF = 1 << 2;
    public static final short IORING_SEND_ZC_REPORT_USAGE = 1 << 3;

    /* Pass through the flags from sqe->file_index to cqe->flags */
    public static final int IORING_MSG_RING_FLAGS_PASS = 1 << 1;


    /*
     * IORING_OP_MSG_RING command types, stored in sqe->addr
     */
    public static final int IORING_MSG_DATA = 0;    /* pass sqe->len as 'res' and off as user_data */
    public static final int IORING_MSG_SEND_FD = 1;    /* send a registered fd to another ring */

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

    /*
     * sqe->timeout_flags
     */
    public static final int IORING_TIMEOUT_ABS = 1 << 0;
    public static final int IORING_TIMEOUT_UPDATE = 1 << 1;
    public static final int IORING_TIMEOUT_BOOTTIME = 1 << 2;
    public static final int IORING_TIMEOUT_REALTIME = 1 << 3;
    public static final int IORING_LINK_TIMEOUT_UPDATE = 1 << 4;
    public static final int IORING_TIMEOUT_ETIME_SUCCESS = 1 << 5;
    public static final int IORING_TIMEOUT_MULTISHOT = 1 << 6;
    public static final int IORING_TIMEOUT_CLOCK_MASK = IORING_TIMEOUT_BOOTTIME | IORING_TIMEOUT_REALTIME;
    public static final int IORING_TIMEOUT_UPDATE_MASK = IORING_TIMEOUT_UPDATE | IORING_LINK_TIMEOUT_UPDATE;

    /*
     * accept flags stored in sqe->ioprio
     */
    public static final int IORING_ACCEPT_MULTISHOT = 1 << 0;

    /**
     * If sqe->file_index is set to this for opcodes that instantiate a new
     * direct descriptor (like openat/openat2/accept), then io_uring will allocate
     * an available direct descriptor instead of having the application pass one
     * in. The picked direct descriptor will be returned in cqe->res, or -ENFILE
     * if the space is full.
     */
    public static final int IORING_FILE_INDEX_ALLOC = ~0;

    /*
     * cqe->flags
     *
     * IORING_CQE_F_BUFFER	If set, the upper 16 bits are the buffer ID
     * IORING_CQE_F_MORE	If set, parent SQE will generate more CQE entries
     * IORING_CQE_F_SOCK_NONEMPTY	If set, more data to read after socket recv
     * IORING_CQE_F_NOTIF	Set for notification CQEs. Can be used to distinct
     * 			them from sends.
     */
    public static final int IORING_CQE_F_BUFFER = 1 << 0;
    public static final int IORING_CQE_F_MORE = 1 << 1;
    public static final int IORING_CQE_F_SOCK_NONEMPTY = 1 << 2;
    public static final int IORING_CQE_F_NOTIF = 1 << 3;


    /*
     * ASYNC_CANCEL flags.
     *
     * IORING_ASYNC_CANCEL_ALL	Cancel all requests that match the given key
     * IORING_ASYNC_CANCEL_FD	Key off 'fd' for cancelation rather than the
     *				request 'user_data'
     * IORING_ASYNC_CANCEL_ANY	Match any request
     * IORING_ASYNC_CANCEL_FD_FIXED	'fd' passed in is a fixed descriptor
     */
    public static final int IORING_ASYNC_CANCEL_ALL = 1 << 0;
    public static final int IORING_ASYNC_CANCEL_FD = 1 << 1;
    public static final int IORING_ASYNC_CANCEL_ANY = 1 << 2;
    public static final int IORING_ASYNC_CANCEL_FD_FIXED = 1 << 3;

    public static final int IOSQE_FIXED_FILE_BIT = 0;
    public static final int IOSQE_IO_DRAIN_BIT = 1;
    public static final int IOSQE_IO_LINK_BIT = 2;
    public static final int IOSQE_IO_HARDLINK_BIT = 3;
    public static final int IOSQE_ASYNC_BIT = 4;
    public static final int IOSQE_BUFFER_SELECT_BIT = 5;
    public static final int IOSQE_CQE_SKIP_SUCCESS_BIT = 6;

    /*
     * sqe->flags
     */
    /*  se fixed fileset */
    public static final int IOSQE_FIXED_FILE = 1 << IOSQE_FIXED_FILE_BIT;
    /* issue after inflight IO */
    public static final int IOSQE_IO_DRAIN = 1 << IOSQE_IO_DRAIN_BIT;
    /* links next sqe */
    public static final int IOSQE_IO_LINK = 1 << IOSQE_IO_LINK_BIT;
    /* like LINK, but stronger */
    public static final int IOSQE_IO_HARDLINK = 1 << IOSQE_IO_HARDLINK_BIT;
    /* always go async */
    public static final int IOSQE_ASYNC = 1 << IOSQE_ASYNC_BIT;
    /* select buffer from sqe->buf_group */
    public static final int IOSQE_BUFFER_SELECT = 1 << IOSQE_BUFFER_SELECT_BIT;
    /* don't post CQE if request succeeded */
    public static final int IOSQE_CQE_SKIP_SUCCESS = 1 << IOSQE_CQE_SKIP_SUCCESS_BIT;


    /*
     * io_uring_setup() flags
     */
    public static final int IORING_SETUP_IOPOLL = 1 << 0;    /* io_context is polled */
    public static final int IORING_SETUP_SQPOLL = 1 << 1;    /* SQ poll thread */
    public static final int IORING_SETUP_SQ_AFF = 1 << 2;    /* sq_thread_cpu is valid */
    public static final int IORING_SETUP_CQSIZE = 1 << 3;    /* app defines CQ size */
    public static final int IORING_SETUP_CLAMP = 1 << 4;    /* clamp SQ/CQ ring sizes */
    public static final int IORING_SETUP_ATTACH_WQ = 1 << 5;    /* attach to existing wq */
    public static final int IORING_SETUP_R_DISABLED = 1 << 6;    /* start with ring disabled */
    public static final int IORING_SETUP_SUBMIT_ALL = 1 << 7;    /* continue submit on error */

    /*
     * sq_ring->flags
     */
    public static final int IORING_SQ_NEED_WAKEUP = 1 << 0; /* needs io_uring_enter wakeup */
    public static final int IORING_SQ_CQ_OVERFLOW = 1 << 1; /* CQ ring is overflown */
    public static final int IORING_SQ_TASKRUN = 1 << 2; /* task should enter the kernel */

    /*
     * io_uring_enter(2) flags
     */
    public static final int IORING_ENTER_GETEVENTS = 1 << 0;
    public static final int IORING_ENTER_SQ_WAKEUP = 1 << 1;
    public static final int IORING_ENTER_SQ_WAIT = 1 << 2;
    public static final int IORING_ENTER_EXT_ARG = 1 << 3;
    public static final int IORING_ENTER_REGISTERED_RING = 1 << 4;
}
