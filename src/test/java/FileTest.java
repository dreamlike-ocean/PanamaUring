import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

import static top.dreamlike.fcntl.fcntl_h.*;
import static top.dreamlike.fcntl.fcntl_h.O_APPEND;
import static top.dreamlike.unistd.unistd_h.*;

public class FileTest {


    public void writeFile(){
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment filePath = session.allocateUtf8String("demo.txt");
            var fd = open(filePath, O_RDWR() |O_CREAT() |O_APPEND());
            if (fd == -1){
                System.out.println("open file error!");
                return;
            }
            var str = "hello panama write file".getBytes(StandardCharsets.UTF_8);
            MemorySegment waitWrite = session.allocateArray(ValueLayout.JAVA_BYTE, str.length);
            waitWrite.copyFrom(MemorySegment.ofArray(str));
            System.out.println(write(fd, waitWrite, waitWrite.byteSize()));
            sync();
            close(fd);
        }
    }
}
