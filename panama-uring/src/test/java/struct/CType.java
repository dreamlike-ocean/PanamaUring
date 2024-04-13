package struct;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.*;

public class CType {
    public static final ValueLayout.OfBoolean C_BOOL$LAYOUT = JAVA_BOOLEAN;
    public static final ValueLayout.OfByte C_CHAR$LAYOUT = JAVA_BYTE;
    public static final ValueLayout.OfShort C_SHORT$LAYOUT = JAVA_SHORT.withByteAlignment(2);
    public static final OfInt C_INT$LAYOUT = JAVA_INT;
    public static final OfLong C_LONG$LAYOUT = JAVA_LONG.withByteAlignment(8);
    public static final OfLong C_LONG_LONG$LAYOUT = JAVA_LONG.withByteAlignment(8);
    public static final OfFloat C_FLOAT$LAYOUT = JAVA_FLOAT.withByteAlignment(8);
    public static final OfDouble C_DOUBLE$LAYOUT = JAVA_DOUBLE.withByteAlignment(8);
    public static final AddressLayout C_POINTER$LAYOUT = ADDRESS.withByteAlignment(8);
    public static final MemorySegment NULL = MemorySegment.NULL;

    public static final OfLong C_SSIZE_t$LAYOUT = JAVA_LONG.withByteAlignment(8);

    public static final OfLong C_SIZE_t$LAYOUT = C_SSIZE_t$LAYOUT;

    public static final AddressLayout POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(0, C_CHAR$LAYOUT));

}
