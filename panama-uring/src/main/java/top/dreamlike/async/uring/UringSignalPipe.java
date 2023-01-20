package top.dreamlike.async.uring;

import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.unistd.unistd_h.pipe;
import static top.dreamlike.nativeLib.unistd.unistd_h.write;

/**
 * 用于唤醒阻塞在wait上的io_uring事件循环线程的pipe fd实现
 * 目前转为使用eventfd实现 maybe性能会好一点？
 * Applications can use an eventfd file descriptor instead of a pipe (see pipe(2)) in all cases where a pipe is used simply to signal events.
 *
 * @deprecated 目前转为使用eventfd实现 wakeup的功能 {@link top.dreamlike.nativeLib.eventfd.EventFd }
 */
@Deprecated
public class UringSignalPipe implements AutoCloseable {
    int writeFd;
    int readFd;
    MemorySegment signalBuffer;

    UringSignalPipe(IOUring uring) {
        MemorySegment pipes = uring.allocator.allocateArray(ValueLayout.JAVA_INT, 2);
        int i = pipe(pipes);
        if (i == -1) {
            System.out.println("error");
            return;
        }
        signalBuffer = uring.allocator.allocate(ValueLayout.JAVA_BYTE, (byte) 1);
        writeFd = pipes.getAtIndex(ValueLayout.JAVA_INT, 1);
        readFd = pipes.getAtIndex(ValueLayout.JAVA_INT, 0);
        int flags = fcntl(writeFd, F_GETFL());
        fcntl(writeFd,F_SETFL(), flags | O_NONBLOCK());

    }

    public void signal(){
        write(writeFd, signalBuffer, signalBuffer.byteSize());
    }

    @Override
    public void close(){
        unistd_h.close(writeFd);
        unistd_h.close(readFd);
    }
}
