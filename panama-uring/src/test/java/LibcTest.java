import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;
import top.dreamlike.panama.uring.nativelib.helper.UnsafeHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class LibcTest {


    @Test
    public void testFile() throws IOException {
        Libc libc = Instance.LIBC;
        File path = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        path.deleteOnExit();
        try (Arena arena = Arena.ofConfined()) {
            String absolutePath = path.getAbsolutePath();
            MemorySegment pathname = arena.allocateFrom(absolutePath);
            int fd = libc.open(pathname, Libc.Fcntl_H.O_RDWR);
            Assert.assertTrue(fd > 0);
            String string = UUID.randomUUID().toString();
            MemorySegment writeBuf = arena.allocateFrom(string);
            int write = libc.write(fd, writeBuf, (int) writeBuf.byteSize());
            Assert.assertTrue(write > 0);

            libc.close(fd);

            fd = libc.open(pathname, Libc.Fcntl_H.O_RDWR);
            MemorySegment readBuf = arena.allocate(string.length());
            int read = libc.read(fd, readBuf, (int) readBuf.byteSize());

            String readbuf = NativeHelper.bufToString(readBuf, string.length());
            Assert.assertEquals(string, readbuf);
        }
    }

    @Test
    public void testGetFd() throws IOException {
        File path = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        path.deleteOnExit();
        try (var fc = FileChannel.open(path.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            int fd = UnsafeHelper.getFd(fc);
            Assert.assertTrue(fd > 0);
        }
    }

}
