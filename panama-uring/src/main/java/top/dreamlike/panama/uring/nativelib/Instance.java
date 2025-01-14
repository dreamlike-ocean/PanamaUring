package top.dreamlike.panama.uring.nativelib;

import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.libs.LibCMalloc;
import top.dreamlike.panama.uring.nativelib.libs.LibEpoll;
import top.dreamlike.panama.uring.nativelib.libs.LibMman;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;

public class Instance {

    public static final StructProxyGenerator STRUCT_PROXY_GENERATOR = new StructProxyGenerator();
    public static final NativeCallGenerator NATIVE_CALL_GENERATOR = new NativeCallGenerator(STRUCT_PROXY_GENERATOR);

    public static final Libc LIBC = NATIVE_CALL_GENERATOR.generate(Libc.class);

    public static final LibCMalloc LIBC_MALLOC = NATIVE_CALL_GENERATOR.generate(LibCMalloc.class);
    public static final LibEpoll LIB_EPOLL = NATIVE_CALL_GENERATOR.generate(LibEpoll.class);
    public static final LibMman LIB_MMAN = NATIVE_CALL_GENERATOR.generate(LibMman.class);

    public static final LibUring LIB_URING;

    static {
        NATIVE_CALL_GENERATOR.indyMode();
        LIB_URING = NATIVE_CALL_GENERATOR.generate(LibUring.class);
    }


}
