package top.dreamlike.panama.uring.nativelib.helper;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class UnsafeHelper {
    private static final Unsafe unsafe;

    private static final MethodHandles.Lookup trusted_lookup;

    private static final MethodHandle getDirectMemorySizeMH;

    public static long getMaxDirectMemory() {
        try {
            return (long) getDirectMemorySizeMH.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = unsafe.staticFieldBase(implLookupField);
            long fieldOffset = unsafe.staticFieldOffset(implLookupField);
            trusted_lookup = ((MethodHandles.Lookup) unsafe.getObject(base, fieldOffset));
            getDirectMemorySizeMH = trusted_lookup.findStatic(Class.forName("jdk.internal.misc.VM"), "maxDirectMemory", MethodType.methodType(long.class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
