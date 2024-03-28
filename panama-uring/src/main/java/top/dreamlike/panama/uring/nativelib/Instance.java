package top.dreamlike.panama.uring.nativelib;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.libs.LibJemalloc;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.libs.Libc;

import java.lang.foreign.MemorySegment;

public class Instance {

    public static final StructProxyGenerator STRUCT_PROXY_GENERATOR = new StructProxyGenerator();
    public static final NativeCallGenerator NATIVE_CALL_GENERATOR = new NativeCallGenerator(STRUCT_PROXY_GENERATOR);

    public static final Libc LIBC = NATIVE_CALL_GENERATOR.generate(Libc.class);
    public static final LibUring LIB_URING = NATIVE_CALL_GENERATOR.generate(LibUring.class);
    public static final LibJemalloc LIB_JEMALLOC = new LibJemalloc() {
        private static final LibJemalloc FFI = NATIVE_CALL_GENERATOR.generate(LibJemalloc.class);
        @Override
        public MemorySegment malloc(long size) {
            return FFI.malloc(size).reinterpret(size);
        }

        @Override
        public void free(MemorySegment ptr) {
            FFI.free(ptr);
        }

        @Override
        public long malloc_usable_size(MemorySegment ptr) {
            return FFI.malloc_usable_size(ptr);
        }
    };

    static {
        NATIVE_CALL_GENERATOR.indyMode();
    }


}
