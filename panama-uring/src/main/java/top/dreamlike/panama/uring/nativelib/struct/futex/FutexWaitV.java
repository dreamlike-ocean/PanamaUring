package top.dreamlike.panama.uring.nativelib.struct.futex;


/**
 * struct futex_waitv - A waiter for vectorized wait
 * @val:	Expected value at uaddr
 * @uaddr:	User address to wait on
 * @flags:	Flags for this waiter
 * @__reserved:	Reserved member to preserve data alignment. Should be 0.
 *
 * struct futex_waitv {
 * __u64 val;
 * __u64 uaddr;
 * __u32 flags;
 * __u32 __reserved;
 * };
 */

public class FutexWaitV {
    private long val;
    private long uaddr;
    private int flags;
    private int __reserved;

    public long getVal() {
        return val;
    }

    public void setVal(long val) {
        this.val = val;
    }

    public long getUaddr() {
        return uaddr;
    }

    public void setUaddr(long uaddr) {
        this.uaddr = uaddr;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int get__reserved() {
        return __reserved;
    }

    public void set__reserved(int __reserved) {
        this.__reserved = __reserved;
    }


}
