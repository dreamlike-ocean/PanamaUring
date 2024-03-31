package top.dreamlike.panama.uring.nativelib.helper;

import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.Libc;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public class DebugHelper {

    private static final String[] errStr = new String[257];

    private static final VarHandle JAVA_BYTE_BIG_ENDIAN = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN).varHandle();

    public static String bufToString(MemorySegment nativeMemory, int length) {
        byte[] bytes = new byte[length];
        MemorySegment.copy(
                nativeMemory,0L,
                MemorySegment.ofArray(bytes),0L, (long)length
        );
        return new String(bytes);
    }

    public static String getErrorStr(int errno) {
        if (errno < 0 || errno >= errStr.length) {
            return "Unknown error";
        }
        return errStr[errno];
    }

    public static String currentErrorStr() {
        return getErrorStr(Instance.LIBC.errorNo().reinterpret(Long.MAX_VALUE).get(ValueLayout.JAVA_INT, 0));
    }

    public static short htons(short host) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return host;
        }
        return Short.reverseBytes(host);
    }

    public static int htonl(int host) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return host;
        }
        return Integer.reverseBytes(host);
    }

    public static int ntohl(int socket) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return socket;
        }
        return Integer.reverseBytes(socket);
    }

    public static short ntohs(short socket) {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return socket;
        }
        return Short.reverseBytes(socket);
    }


    static {
        for (int i = 0; i < errStr.length; i++) {
            MemorySegment segment = Instance.LIBC.strerror(i);
            segment = segment.reinterpret(Long.MAX_VALUE);
            errStr[i] = segment.getString(0);
        }

    }


}
