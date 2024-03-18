package top.dreamlike.panama.nativelib.libs;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.nativelib.struct.iovec.Iovec;
import top.dreamlike.panama.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.nativelib.struct.liburing.IoUringParams;
import top.dreamlike.panama.nativelib.struct.liburing.IoUringSqe;
import top.dreamlike.panama.nativelib.struct.sigset.SigsetType;
import top.dreamlike.panama.nativelib.struct.socket.MsgHdr;
import top.dreamlike.panama.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

@CLib("liburing-ffi.so")
public interface LibUring {
    //跟队列本身相关的操作
    int io_uring_queue_init(int entries, @Pointer IoUring ring, int flags);

    int io_uring_queue_init_params(int entries, @Pointer IoUring ring, @Pointer IoUringParams p);

    void io_uring_queue_exit(@Pointer IoUring ring);

    //cqe相关的操作
    @NativeFunction(fast = true)
    int io_uring_peek_batch_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqes, int count);

    @NativeFunction(fast = true)
    int io_uring_peek_cqe(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr);

    int io_uring_wait_cqes(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, int wait_nr, @Pointer KernelTime64Type ts, @Pointer SigsetType sigmask);

    int io_uring_wait_cqe_timeout(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, @Pointer KernelTime64Type ts);

    int io_uring_submit(@Pointer IoUring ring);

    int io_uring_submit_and_wait(@Pointer IoUring ring, int wait_nr);

    int io_uring_submit_and_wait_io_uring_wait_cqes(@Pointer IoUring ring, @Pointer MemorySegment cqe_ptr, int wait_nr, @Pointer KernelTime64Type ts, @Pointer SigsetType sigmask);

    @NativeFunction(fast = true)
    void io_uring_cq_advance(@Pointer IoUring ring, int nr);

    int io_uring_register_buffers(@Pointer IoUring ring, @Pointer NativeArrayPointer<Iovec> iovecs, int nr_iovecs);
    int io_uring_unregister_buffers(@Pointer IoUring ring);

    int io_uring_register_files(@Pointer IoUring ring,/*const int * */ @Pointer MemorySegment files, int nr_files);
    int io_uring_unregister_files(@Pointer IoUring ring);

    int io_uring_register_eventfd(@Pointer IoUring ring, int fd);
    int io_uring_register_eventfd_async(@Pointer IoUring ring, int fd);
    int io_uring_unregister_eventfd(@Pointer IoUring ring);

    int io_uring_register_ring_fd(@Pointer IoUring ring);
    int io_uring_unregister_ring_fd(@Pointer IoUring ring);

    int io_uring_close_ring_fd(@Pointer IoUring ring);

    int io_uring_register_buf_ring(@Pointer IoUring ring, @Pointer MemorySegment br, int br_flags);
    int io_uring_unregister_buf_ring(@Pointer IoUring ring, int bgid);
    int io_uring_buf_ring_head(@Pointer IoUring ring, int buf_group, /* unsigned * head*/ @Pointer MemorySegment head);
    int io_uring_submit_and_get_events(@Pointer IoUring ring);
    int io_uring_get_events(@Pointer IoUring ring);

    //sqe相关的操作
    @NativeFunction(fast = true, returnIsPointer = true)
    IoUringSqe io_uring_get_sqe(@Pointer IoUring ring);

    default void io_uring_prep_rw(int opcode, @Pointer IoUringSqe sqe, int fd, MemorySegment addr, int len, long offset) {
        if (!StructProxyGenerator.isNativeStruct(sqe)) {
            throw new StructException("sqe is not struct,pleace call StructProxyGenerator::enhance before calling native function");
        }
        sqe.setOpcode((byte) opcode);
        sqe.setFlags((byte) 0);
        sqe.setIoprio((short) 0);
        sqe.setFd(fd);
        sqe.setOffset(offset);
        sqe.setAddr(addr == null ? 0 : addr.address());
        sqe.setLen(len);
        sqe.setFlagsInFlagsUnion(0);
        sqe.setBufIndex((short) 0);
        sqe.setPersonality((short) 0);
        sqe.setFileIndex(0);
        sqe.setAddr3(0);
        sqe.setPad2(0);
    }


    /**
     * io_uring_prep_splice() - Either @fd_in or @fd_out must be a pipe.
     * - If @fd_in refers to a pipe, @off_in is ignored and must be set to -1.
     * - If @fd_in does not refer to a pipe and @off_in is -1, then @nbytes are read
     * from @fd_in starting from the file offset, which is incremented by the
     * number of bytes read.
     * - If @fd_in does not refer to a pipe and @off_in is not -1, then the starting
     * offset of @fd_in will be @off_in.
     * <p>
     * This splice operation can be used to implement sendfile by splicing to an
     * intermediate pipe first, then splice to the final destination.
     * In fact, the implementation of sendfile in kernel uses splice internally.
     * NOTE that even if fd_in or fd_out refers to a pipe, the splice operation
     * can still fail with EINVAL if one of the fd doesn't explicitly support splice
     * operation, e.g. reading from terminal is unsupported from kernel 5.7 to 5.11.
     * Check issue #291 for more information.
     */
    default void io_uring_prep_splice(@Pointer IoUringSqe sqe,
                                      int fd_in, long off_in,
                                      int fd_out, long off_out,
                                      int nbytes,
                                      int splice_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SPLICE, sqe, fd_out, MemorySegment.NULL, nbytes, off_out);
        sqe.setSpliceOffIn(off_in);
        sqe.setSpliceFdIn(fd_in);
        sqe.setSpliceFlags(splice_flags);
    }

    default void io_uring_prep_tee(@Pointer IoUringSqe sqe,
                                   int fd_in, int fd_out,
                                   int nbytes,
                                   int splice_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TEE, sqe, fd_out, MemorySegment.NULL, nbytes, 0);
        sqe.setSpliceOffIn(0);
        sqe.setSpliceFdIn(fd_in);
        sqe.setSpliceFlags(splice_flags);
    }

    default void io_uring_prep_readv(@Pointer IoUringSqe sqe, int fd, @Pointer Iovec iovec, int count, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
    }

    default void io_uring_prep_readv2(@Pointer IoUringSqe sqe, int fd, @Pointer Iovec iovec, int count, long offset, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_read_fixed(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset, int buf_index) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ_FIXED, sqe, fd, buf, nbytes, offset);
        sqe.setBufIndex((short) buf_index);
    }


    default void io_uring_prep_writev(@Pointer IoUringSqe sqe, int fd, @Pointer Iovec iovec, int count, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITEV, sqe, fd, StructProxyGenerator.findMemorySegment(iovec), count, offset);
    }

    default void io_uring_prep_writev2(@Pointer IoUringSqe sqe, int fd, @Pointer Iovec iovec, int count, long offset, int flags) {
        io_uring_prep_writev(sqe, fd, iovec, count, offset);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_write_fixed(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset, int buf_index) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITE_FIXED, sqe, fd, buf, nbytes, offset);
        sqe.setBufIndex((short) buf_index);
    }

    default void io_uring_prep_recvmsg(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_RECVMSG, sqe, fd, StructProxyGenerator.findMemorySegment(msg), 1, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_recvmsg_multishot(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_recvmsg(sqe, fd, msg, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECV_MULTISHOT));
    }

    default void io_uring_prep_sendmsg(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msg, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SENDMSG, sqe, fd, StructProxyGenerator.findMemorySegment(msg), 1, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_poll_add(@Pointer IoUringSqe sqe, int fd, int poll_mask) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_ADD, sqe, fd, MemorySegment.NULL, 0, 0);
        //是个union 直接写rwflags就行了
        sqe.setFlagsInFlagsUnion(io_uring_prep_poll_mask(poll_mask));
    }

    default void io_uring_prep_poll_multishot(@Pointer IoUringSqe sqe, int fd, int poll_mask) {
        io_uring_prep_poll_add(sqe, fd, poll_mask);
        sqe.setLen(IoUringConstant.IORING_POLL_ADD_MULTI);
    }

    default void io_uring_prep_poll_remove(@Pointer IoUringSqe sqe, int user_data) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_REMOVE, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
    }

    default void io_uring_prep_poll_update(@Pointer IoUringSqe sqe, long old_user_data, long new_user_data, int poll_mask, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_POLL_REMOVE, sqe, -1, MemorySegment.NULL, flags, new_user_data);
        sqe.setAddr(old_user_data);
        sqe.setFlagsInFlagsUnion(io_uring_prep_poll_mask(poll_mask));
    }

    default void io_uring_prep_fsync(@Pointer IoUringSqe sqe, int fd, int fsync_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FSYNC, sqe, fd, MemorySegment.NULL, 0, 0);
        sqe.setFlagsInFlagsUnion(fsync_flags);
    }

    default void io_uring_prep_nop(@Pointer IoUringSqe sqe) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_NOP, sqe, -1, MemorySegment.NULL, 0, 0);
    }

    default void io_uring_prep_timeout(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, int count, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT, sqe, -1, StructProxyGenerator.findMemorySegment(ts), 1, count);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_timeout_remove(@Pointer IoUringSqe sqe, int user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT_REMOVE, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_timeout_update(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, long user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_TIMEOUT_REMOVE, sqe, -1, MemorySegment.NULL, 0, StructProxyGenerator.findMemorySegment(ts).address());
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags | IoUringConstant.IORING_TIMEOUT_UPDATE);
    }

    /**
     * addr给的宽松一点 以支持v4 v6
     */
    default void io_uring_prep_accept(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ACCEPT, sqe, fd, addr, 0, addr.address());
        sqe.setFlagsInFlagsUnion(flags);
    }

    /* accept directly into the fixed file table */
    default void io_uring_prep_multishot_accept(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags, int file_index) {
        io_uring_prep_accept(sqe, fd, addr, addrlen, flags);
        /* offset by 1 for allocation */
        if (file_index == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            file_index--;
        }
        sqe.setFileIndex(file_index + 1);
    }

    default void io_uring_prep_multishot_accept(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_accept(sqe, fd, addr, addrlen, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_ACCEPT_MULTISHOT));
    }

    default void io_uring_prep_multishot_accept_direct(@Pointer IoUringSqe sqe, int fd,/* struct sockaddr * */ @Pointer MemorySegment addr, /* socklen_t*  */@Pointer MemorySegment addrlen, int flags) {
        io_uring_prep_multishot_accept(sqe, fd, addr, addrlen, flags);
        sqe.setFileIndex(IoUringConstant.IORING_FILE_INDEX_ALLOC);
    }

    default void io_uring_prep_cancel64(@Pointer IoUringSqe sqe, long user_data, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL, sqe, -1, MemorySegment.NULL, 0, 0);
        sqe.setAddr(user_data);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_cancel_fd(@Pointer IoUringSqe sqe, int fd, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL, sqe, fd, MemorySegment.NULL, 0, 0);
        sqe.setFlagsInFlagsUnion(flags | IoUringConstant.IORING_ASYNC_CANCEL_FD);
    }

    default void io_uring_prep_link_timeout(@Pointer IoUringSqe sqe, @Pointer KernelTime64Type ts, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_LINK_TIMEOUT, sqe, -1, StructProxyGenerator.findMemorySegment(ts), 1, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_connect(@Pointer IoUringSqe sqe, int fd, /* struct sockaddr * */ @Pointer MemorySegment addr, int addrlen) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_CONNECT, sqe, fd, addr, 0, addrlen);
    }

    default void io_uring_prep_files_update(@Pointer IoUringSqe sqe,/* int *fds */ @Pointer MemorySegment fds, int nr_fds, int offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FILES_UPDATE, sqe, -1, fds, nr_fds, offset);
    }

    default void io_uring_prep_fallocate(@Pointer IoUringSqe sqe, int fd, int mode, long offset, long len) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FALLOCATE, sqe, fd, MemorySegment.NULL, mode, offset);
        sqe.setAddr(len);
    }

    default void io_uring_prep_openat(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mode) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_OPENAT, sqe, dfd, path, mode, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_openat_direct(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mode, int fileIndex) {
        io_uring_prep_openat(sqe, dfd, path, flags, mode);
        if (fileIndex == IoUringConstant.IORING_FILE_INDEX_ALLOC) {
            fileIndex--;
        }
        sqe.setFileIndex(fileIndex + 1);
    }

    default void io_uring_prep_close(@Pointer IoUringSqe sqe, int fd) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_CLOSE, sqe, fd, MemorySegment.NULL, 0, 0);
    }

    default void io_uring_prep_close_direct(@Pointer IoUringSqe sqe, int fileIndex) {
        io_uring_prep_close(sqe, 0);
        sqe.setFileIndex(fileIndex + 1);
    }

    default void io_uring_prep_read(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ_FIXED, sqe, fd, buf, nbytes, offset);
    }

    default void io_uring_prep_read_multishot(@Pointer IoUringSqe sqe, int fd, int nbytes, long offset, int buf_group) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_READ_MULTISHOT, sqe, fd, MemorySegment.NULL, nbytes, offset);
        sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
        sqe.setBufGroup((short) buf_group);
    }

    default void io_uring_prep_write(@Pointer IoUringSqe sqe, int fd, @Pointer MemorySegment buf, int nbytes, long offset) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_WRITE_FIXED, sqe, fd, buf, nbytes, offset);
    }

    default void io_uring_prep_statx(@Pointer IoUringSqe sqe, int dfd, /*const char * path*/@Pointer MemorySegment path, int flags, int mask,/* struct statx *statxbuf */ @Pointer MemorySegment statxbuf) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_STATX, sqe, dfd, path, mask, statxbuf.address());
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_fadvise(@Pointer IoUringSqe sqe, int fd, long offset, long len, int advice) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_FADVISE, sqe, fd, MemorySegment.NULL, advice, offset);
        sqe.setFlagsInFlagsUnion(advice);
    }

    default void io_uring_prep_madvise(@Pointer IoUringSqe sqe, @Pointer MemorySegment addr, long length, int advice) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_MADVISE, sqe, -1, addr, (int) length, 0);
        sqe.setFlagsInFlagsUnion(advice);
    }

    default void io_uring_prep_send(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int nbytes, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SEND, sqe, sockfd, buf, nbytes, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_send_set_addr(@Pointer IoUringSqe sqe,/*const struct sockaddr *dest_addr*/ @Pointer MemorySegment addr, short addrlen) {
        sqe.setAddr2(addr.address());
        sqe.setLen(addrlen);
    }

    default void io_uring_prep_sendto(@Pointer IoUringSqe sqe, int socketfd, @Pointer MemorySegment buf, int len, int flags,
            /*const struct sockaddr *addr*/ @Pointer MemorySegment addr, int addrlen) {
        io_uring_prep_send(sqe, socketfd, buf, len, flags);
        io_uring_prep_send_set_addr(sqe, addr, (short) addrlen);
    }

    default void io_uring_prep_send_zc(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags, int zc_flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_SEND_ZC, sqe, sockfd, buf, len, 0);
        sqe.setFlagsInFlagsUnion(flags);
        sqe.setIoprio((short) zc_flags);
    }

    default void io_uring_prep_send_zc_fixed(@Pointer IoUringSqe sqe, int sockfd,
                                             @Pointer MemorySegment buf, int len, int flags, int zc_flags,
                                             int buf_index) {
        io_uring_prep_send_zc(sqe, sockfd, buf, len, flags, zc_flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECVSEND_FIXED_BUF));
        sqe.setBufIndex((short) buf_index);
    }

    default void io_uring_prep_sendmsg_zc(@Pointer IoUringSqe sqe, int fd, @Pointer MsgHdr msgHdr, int flags) {
        io_uring_prep_sendmsg(sqe, fd, msgHdr, flags);
        sqe.setOpcode(IoUringConstant.Opcode.IORING_OP_SENDMSG);
    }

    default void io_uring_prep_recv(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags) {
        io_uring_prep_rw(IoUringConstant.Opcode.IORING_OP_RECV, sqe, sockfd, buf, len, 0);
        sqe.setFlagsInFlagsUnion(flags);
    }

    default void io_uring_prep_recv_multishot(@Pointer IoUringSqe sqe, int sockfd, @Pointer MemorySegment buf, int len, int flags) {
        io_uring_prep_recv(sqe, sockfd, buf, len, flags);
        sqe.setIoprio((short) (sqe.getIoprio() | IoUringConstant.IORING_RECV_MULTISHOT));
    }




    private int io_uring_prep_poll_mask(int poll_mask) {
        //判断大小端
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
//            用于交换32位整数的高位字节和低位字节。
            poll_mask = Integer.reverseBytes(poll_mask);
        }
        return poll_mask;
    }


}
