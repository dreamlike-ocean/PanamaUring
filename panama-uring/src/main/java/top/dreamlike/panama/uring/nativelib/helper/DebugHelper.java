package top.dreamlike.panama.uring.nativelib.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.ErrorKernelVersionException;
import top.dreamlike.panama.uring.trait.OwnershipResource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;

public class DebugHelper {

    public static String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir");

    private static final Logger logger = LogManager.getLogger(DebugHelper.class);

    private static final String[] errStr = new String[257];

    private static final VarHandle JAVA_BYTE_BIG_ENDIAN = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.BIG_ENDIAN).varHandle();

    private static final String osVersion;

    private static final String arch;

    private static final String osName;

    public static final int currentLinuxMajor;

    public static final int currentLinuxMinor;

    private static final boolean osLinux;


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


    public static  <T> void dropBatch(List<OwnershipResource<T>> memories) {
        for (OwnershipResource<T> resource : memories) {
            try (resource) {
                resource.drop();
            } catch (Throwable throwable) {
                logger.error("Drop memory failed", throwable);
            }
        }
    }


    public static <T> T enhanceCheck(T afterProxy, Class<T> nativeInterface) {
        return (T) Proxy.newProxyInstance(nativeInterface.getClassLoader(), new Class[]{nativeInterface}, (Object proxy, Method method, Object[] args) -> {
            KernelVersionLimit annotation = Optional.ofNullable(method.getAnnotation(KernelVersionLimit.class)).orElse(nativeInterface.getAnnotation(KernelVersionLimit.class));
            if (annotation != null) {
                if (!osLinux) {
                    logger.error("This method is only supported on Linux");
                    throw new ErrorKernelVersionException();
                }
                if (!allowCurrentLinuxVersion(annotation.major(), annotation.minor())) {
                    logger.error("This method is only supported on Linux kernel version {}.{}, current version is {}.{}",
                            annotation.major(), annotation.minor(), currentLinuxMajor, currentLinuxMinor);
                    throw new ErrorKernelVersionException(annotation.major(), annotation.minor());
                }
            }
            return method.invoke(afterProxy, args);
        });
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
