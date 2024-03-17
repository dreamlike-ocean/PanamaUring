package nativelib;

import top.dreamlike.helper.StackValue;
import top.dreamlike.nativeLib.fcntl.fcntl_h;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class SpliceExample {

    public static void main(String[] args) throws Throwable {
        try (StackValue stackValue = StackValue.currentStack()) {
            MemorySegment path = stackValue.allocateFrom("README.md");
            int fd = fcntl_h.open(path, fcntl_h.O_RDONLY());
            stackValue.reset();

            path = stackValue.allocateFrom("demo.txt");
            int targetFd = fcntl_h.open(path, fcntl_h.O_WRONLY());

            MemorySegment pipeFds = stackValue.allocateFrom(ValueLayout.JAVA_INT, 0, 0);
            int syscallRes = unistd_h.pipe(pipeFds);
            int pipeReadFd = pipeFds.getAtIndex(ValueLayout.JAVA_INT, 0);
            int pipeWriteFd = pipeFds.getAtIndex(ValueLayout.JAVA_INT, 1);
            stackValue.reset();
            var firstSpliceSize = fcntl_h.splice(fd, MemorySegment.NULL, pipeWriteFd, MemorySegment.NULL, 1024, fcntl_h.SPLICE_F_MOVE());
            var secondSpliceSize = fcntl_h.splice(pipeReadFd, MemorySegment.NULL, targetFd, MemorySegment.NULL, 1024, fcntl_h.SPLICE_F_MOVE());
            System.out.println(STR. "firstSpliceSize: \{ firstSpliceSize }, secondSpliceSize: \{ secondSpliceSize }" );
        }
    }
}
