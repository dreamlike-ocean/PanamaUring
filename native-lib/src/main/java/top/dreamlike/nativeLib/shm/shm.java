package top.dreamlike.nativeLib.shm;

import top.dreamlike.helper.NativeHelper;

import java.lang.foreign.MemorySegment;

import static top.dreamlike.nativeLib.shm.Constants$0.shm_open$MH;
import static top.dreamlike.nativeLib.shm.Constants$0.shm_unlink$MH;

public class shm {


    static {
        NativeHelper.loadSo("librt-2.31");
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
