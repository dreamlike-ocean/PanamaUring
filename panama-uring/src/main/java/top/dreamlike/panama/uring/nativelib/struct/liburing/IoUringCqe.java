package top.dreamlike.panama.uring.nativelib.struct.liburing;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;

import java.lang.foreign.MemorySegment;

import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_CQE_F_MORE;

public class IoUringCqe {
    public long user_data;
    public int res;
    /**
     *  is possible for a single submission to result in multiple completions (e.g. io_uring_prep_multishot_accept(3));
     *  this is known as multishot. Errors on a multishot SQE will typically terminate the work request;
     *  a multishot SQE will set IORING_CQE_F_MORE high in generated CQEs so long as it remains active.
     *  A CQE without this flag indicates that the multishot is no longer operational, and must be reposted if further events are desired.
     *  Overflow of the completion queue will usually result in a drop of any firing multishot.
     */
    public int flags;
    @NativeArrayMark(size = long.class, length = 0)
    public MemorySegment big_cqe;

    public boolean isMultiShot() {
        return (getFlags() & IoUringConstant.IORING_CQE_F_MORE) != 0;
    }

    public long getUser_data() {
        return user_data;
    }

    public void setUser_data(long user_data) {
        this.user_data = user_data;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public MemorySegment getBig_cqe() {
        return big_cqe;
    }

    public void setBig_cqe(MemorySegment big_cqe) {
        this.big_cqe = big_cqe;
    }

    public boolean hasMore() {
        return (getFlags() & IORING_CQE_F_MORE) != 0;
    }
}
