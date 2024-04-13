package top.dreamlike.panama.uring.sync.fd;

import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.sync.trait.PollableFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicBoolean;

public class PipeFd implements PollableFd, NativeFd {

    private final int readSide;

    private final int writeSide;

    private AtomicBoolean hasClosed = new AtomicBoolean(false);

    public PipeFd() {
        try (OwnershipMemory memory = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.JAVA_INT.byteSize() * 2)) {
            int syscallRes = Instance.LIBC.pipe(memory.resource());
            if (syscallRes != 0) {
                throw new IllegalArgumentException("pipe failure!, reason "+ NativeHelper.currentErrorStr());
            }
            readSide = memory.resource().getAtIndex(ValueLayout.JAVA_INT, 0);
            writeSide = memory.resource().getAtIndex(ValueLayout.JAVA_INT, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int fd() {
        return -1;
    }

    @Override
    public int writeFd() {
        return writeSide;
    }

    @Override
    public int readFd() {
        return readSide;
    }

    @Override
    public void close() {
        if (hasClosed.compareAndSet(false, true)) {
            Instance.LIBC.close(readFd());
            Instance.LIBC.close(writeFd());
        }

    }
}
