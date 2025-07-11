package top.dreamlike.panama.uring.nativelib.helper;

import top.dreamlike.panama.uring.helper.unsafe.TrustedLookup;

import java.io.FileDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.channels.FileChannel;

public class UnsafeHelper {

    private static final MethodHandles.Lookup trusted_lookup;

    private static final MethodHandle getDirectMemorySizeMH;

    private static final MethodHandle GET_FD_MH;

    static {
        try {
            MethodHandles.Lookup trustedLookup = TrustedLookup.TREUSTED_LOOKUP;
            trusted_lookup = trustedLookup;
            getDirectMemorySizeMH = trusted_lookup.findStatic(Class.forName("jdk.internal.misc.VM"), "maxDirectMemory", MethodType.methodType(long.class));
            var getFileDescriptorMH = trusted_lookup.findVarHandle(
                    trustedLookup.findClass("sun.nio.ch.FileChannelImpl"),
                    "fd",
                    FileDescriptor.class
            ).toMethodHandle(VarHandle.AccessMode.GET);

            var getFdMH = trusted_lookup.findVarHandle(
                    FileDescriptor.class,
                    "fd",
                    int.class
            ).toMethodHandle(VarHandle.AccessMode.GET);

            MethodHandle methodHandle = MethodHandles.filterReturnValue(
                    getFileDescriptorMH,
                    getFdMH
            );

            GET_FD_MH = methodHandle.asType(methodHandle.type().changeParameterType(0, FileChannel.class));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long getMaxDirectMemory() {
        try {
            return (long) getDirectMemorySizeMH.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getFd(FileChannel channel) {
        try {
            return (int) GET_FD_MH.invokeExact(channel);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
