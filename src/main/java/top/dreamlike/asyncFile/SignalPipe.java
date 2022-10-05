package top.dreamlike.asyncFile;

import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;


import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.unistd.unistd_h.*;

public class SignalPipe implements AutoCloseable{
    int writeFd;
    int readFd;
    MemorySegment signalBuffer;

    SignalPipe(IOUring uring){
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
