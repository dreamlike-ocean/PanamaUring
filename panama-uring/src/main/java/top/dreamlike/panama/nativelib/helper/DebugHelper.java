package top.dreamlike.panama.nativelib.helper;

import java.lang.foreign.MemorySegment;

public class DebugHelper {

    public static String bufToString(MemorySegment nativeMemory, int length) {
        byte[] bytes = new byte[length];
        MemorySegment.copy(
                nativeMemory,0L,
                MemorySegment.ofArray(bytes),0L, (long)length
        );
        return new String(bytes);
    }
}
