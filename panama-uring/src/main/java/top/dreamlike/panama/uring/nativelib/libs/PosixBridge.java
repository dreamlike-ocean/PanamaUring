package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.uring.helper.JemallocAllocator;
import top.dreamlike.panama.uring.nativelib.Instance;

import java.io.File;
import java.lang.foreign.MemorySegment;

public class PosixBridge {

    public static int open(File file, int flags) {
        MemorySegment cPath = JemallocAllocator.INSTANCE.allocateFrom(file.getAbsolutePath());
        try {
            return Instance.LIBC.open(cPath, flags);
        } finally {
            JemallocAllocator.INSTANCE.free(cPath);
        }
    }

}
