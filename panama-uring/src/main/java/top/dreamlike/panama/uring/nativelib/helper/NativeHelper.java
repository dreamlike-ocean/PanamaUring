package top.dreamlike.panama.uring.nativelib.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.helper.LambdaHelper;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.trait.OwnershipMemory;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.IntSupplier;

import static top.dreamlike.panama.uring.nativelib.Instance.LIBC;
import static top.dreamlike.panama.uring.nativelib.libs.Libc.Socket_H.OptName.SO_REUSEADDR;
import static top.dreamlike.panama.uring.nativelib.libs.Libc.Socket_H.SetSockOpt.SOL_SOCKET;

public class NativeHelper {

    public static String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir");

    public static boolean enableOpVersionCheck = System.getProperty("enable-detect-os-version", "true").equalsIgnoreCase("true");

    private final static Logger logger = LoggerFactory.getLogger(NativeHelper.class);

    private static final String[] errStr = new String[257];

    private static final VarHandle JAVA_BYTE_BIG_ENDIAN = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN).varHandle();

    private static final String osVersion;

    private static final String arch;

    private static final String osName;

    public static final int currentLinuxMajor;

    public static final int currentLinuxMinor;

    private static final boolean osLinux;

    private static final boolean isSkipSameEventLoopCheck = System.getProperty("top.dreamlike.panama.uring.skipSameEventLoopCheck", "false").equalsIgnoreCase("true");

    public static String bufToString(MemorySegment nativeMemory, int length) {
        byte[] bytes = new byte[length];
        MemorySegment.copy(
                nativeMemory, 0L,
                MemorySegment.ofArray(bytes), 0L, (long) length
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
        return getErrorStr(errorno());
    }

    public static int errorno() {
        return Instance.LIBC.errorNo().reinterpret(Long.MAX_VALUE).get(ValueLayout.JAVA_INT, 0);
    }

    public static int nativeCall(IntSupplier nativeFunction) {
        int syscallRes = nativeFunction.getAsInt();
        if (syscallRes < 0) {
            throw new SyscallException(-NativeHelper.errorno());
        }
        return syscallRes;
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

    public static int makeNonBlock(int fd) {
        return Instance.LIBC.fcntl(fd, Libc.Fcntl_H.F_SETFL, Libc.Fcntl_H.O_NONBLOCK);
    }

    public static int socketSysCall(SocketAddress address) {
        int domain = switch (address) {
            case UnixDomainSocketAddress _ -> Libc.Socket_H.Domain.AF_UNIX;
            case InetSocketAddress inetSocketAddress -> switch (inetSocketAddress.getAddress()) {
                case Inet4Address _ -> Libc.Socket_H.Domain.AF_INET;
                case Inet6Address _ -> Libc.Socket_H.Domain.AF_INET6;
                default -> throw new IllegalStateException("Unexpected value: " + inetSocketAddress);
            };
            default -> throw new IllegalStateException("Unexpected value: " + address);
        };
        int type = Libc.Socket_H.Type.SOCK_STREAM;
        int fd = LIBC.socket(domain, type, 0);
        if (fd < 0) {
            throw new IllegalArgumentException("socket error, reason： " + NativeHelper.currentErrorStr());
        }
        try (OwnershipMemory ownershipMemory = Instance.LIB_JEMALLOC.mallocMemory(ValueLayout.JAVA_INT.byteSize())) {
            ownershipMemory.resource().set(ValueLayout.JAVA_INT, 0, 1);
            LIBC.setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, ownershipMemory.resource(), (int) ValueLayout.JAVA_INT.byteSize());
        } catch (Exception _) {
        }
        return fd;
    }

    public static <T> void dropBatch(List<OwnershipResource<T>> memories) {
        for (OwnershipResource<T> resource : memories) {
            try (resource) {
                resource.drop();
            } catch (Throwable throwable) {
                logger.error("Drop memory failed", throwable);
            }
        }
    }

    public static boolean inSameEventLoop(IoUringEventLoop eventLoop, Object o) {
        if (isSkipSameEventLoopCheck) {
            return true;
        }

        Class<?> aClass = o.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            if (!IoUringOperator.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            var loop = ((IoUringOperator) LambdaHelper.runWithThrowable(() -> field.get(o))).owner();
            if (loop != eventLoop) {
                return false;
            }
        }
        return true;
    }

    public static boolean isLinux() {
        return osLinux;
    }

    public static boolean allowCurrentLinuxVersion(int major, int minor) {
        return currentLinuxMajor > major || (currentLinuxMajor == major && currentLinuxMinor >= minor);
    }

    static {
        for (int i = 0; i < errStr.length; i++) {
            MemorySegment segment = Instance.LIBC.strerror(i);
            segment = segment.reinterpret(Long.MAX_VALUE);
            errStr[i] = segment.getString(0);
        }
        osVersion = System.getProperty("os.version", "");
        osName = System.getProperty("os.name", "");
        String arch0 = System.getProperty("os.arch", "x86_64" /*most java users are x86_64*/);
        if (arch0.equals("amd64")) {
            arch0 = "x86_64";
        }
        arch = arch0;
        String os = osName.toLowerCase();
        osLinux = os.contains("linux");
        int major = -1;
        int minor = -1;
        if (osLinux) {
            String[] split = osVersion.split("\\.");
            if (split.length >= 2) {
                try {
                    major = Integer.parseInt(split[0]);
                    minor = Integer.parseInt(split[1]);
                } catch (NumberFormatException ignore) {
                }
            }
            if (minor == -1) {
                major = -1;
            }


        }

        currentLinuxMajor = major;
        currentLinuxMinor = minor;
    }


}
