import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.LibMman;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.libs.PosixBridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class LibmmanTest {

    private static final Logger log = LoggerFactory.getLogger(LibmmanTest.class);

    @Test
    public void testMmap() throws IOException {
        File tempFile = File.createTempFile("test4mmap", ".txt");
        tempFile.deleteOnExit();
        String content = "hello mmap";
        int fileSize = 0;
        try (var output = new FileOutputStream(tempFile)){
            output.write(content.getBytes());
            fileSize = (int) output.getChannel().size();
        }
        Assert.assertEquals(content.length(), fileSize);

        var fd = PosixBridge.open(tempFile, Libc.Fcntl_H.O_RDWR);
        Assert.assertTrue(fd > 0);

        LibMman libMman = Instance.LIB_MMAN;
        MemorySegment mmapBasePointer = libMman.mmap(MemorySegment.NULL, fileSize, LibMman.Prot.PROT_READ, LibMman.Flag.MAP_SHARED, fd, 0);

        Assert.assertNotNull(mmapBasePointer);
        Assert.assertNotEquals(-1, mmapBasePointer.address());
        log.info("mmap success: {}", mmapBasePointer);

        long pageStart = libMman.alignPageSizeAddress(mmapBasePointer);
        //测试madvise
        int madvise = libMman.madvise(MemorySegment.ofAddress(pageStart), LibMman.PAGE_SIZE, LibMman.Advice.MADV_SEQUENTIAL | LibMman.Advice.MADV_WILLNEED);
        Assert.assertEquals(0, madvise);

        //测试read
        mmapBasePointer = mmapBasePointer.reinterpret(content.length());
        Assert.assertEquals(content, new String(mmapBasePointer.toArray(ValueLayout.JAVA_BYTE)));

        //mprotected设置可写
        int mprotected = libMman.mprotect(MemorySegment.ofAddress(pageStart), LibMman.PAGE_SIZE, LibMman.Prot.PROT_READ | LibMman.Prot.PROT_WRITE);
        Assert.assertEquals(0, mprotected);

        //msync测试看看 能不能回刷
        int msync = libMman.msync(mmapBasePointer, fileSize, LibMman.MsyncFlag.MS_SYNC);
        Assert.assertEquals(0, msync);

        mmapBasePointer.set(ValueLayout.JAVA_BYTE, 0, (byte) 'y');
        try (var output = new FileInputStream(tempFile)) {
            Assert.assertEquals('y', (char) output.read());
        }

        //release mmap
        int munmap = libMman.munmap(mmapBasePointer, fileSize);
        Assert.assertEquals(0, munmap);

    }

}
