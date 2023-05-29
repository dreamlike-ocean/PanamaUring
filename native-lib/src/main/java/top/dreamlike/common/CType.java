package top.dreamlike.common;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.*;

public class CType {
    public static final ValueLayout.OfBoolean C_BOOL$LAYOUT = JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR$LAYOUT = JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT$LAYOUT = JAVA_SHORT.withBitAlignment(16);
    public static final  OfInt C_INT$LAYOUT = JAVA_INT.withBitAlignment(32);
    public static final  OfLong C_LONG$LAYOUT = JAVA_LONG.withBitAlignment(64);
    public static final  OfLong C_LONG_LONG$LAYOUT = JAVA_LONG.withBitAlignment(64);
    public static final  OfFloat C_FLOAT$LAYOUT = JAVA_FLOAT.withBitAlignment(32);
    public static final  OfDouble C_DOUBLE$LAYOUT = JAVA_DOUBLE.withBitAlignment(64);
    public static final  OfAddress C_POINTER$LAYOUT = ADDRESS.withBitAlignment(64);
    static final MemorySegment NULL = MemorySegment.NULL;

}
