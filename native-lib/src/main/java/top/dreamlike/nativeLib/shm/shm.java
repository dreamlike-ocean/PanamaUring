package top.dreamlike.nativeLib.shm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;

import static top.dreamlike.nativeLib.shm.Constants$0.shm_open$MH;
import static top.dreamlike.nativeLib.shm.Constants$0.shm_unlink$MH;

public class shm {


    static {
        try {
            InputStream is = shm.class.getResourceAsStream("/librt-2.31.so");
            File file = File.createTempFile("librt-2.31", ".so");
            OutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
            System.load(file.getAbsolutePath());
            file.deleteOnExit();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static int shm_open(MemorySegment name, int oflag, int mode) {
        try {
            return ((int) shm_open$MH.invoke(name, oflag, mode));
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    public static int shm_unlike(MemorySegment name) {
        try {
            return ((int) shm_unlink$MH.invoke(name));
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }


}
