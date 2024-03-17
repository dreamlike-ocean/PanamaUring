package top.dreamlike.panama.nativelib;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.nativelib.libs.Libc;

public class Instance {

    public static final StructProxyGenerator STRUCT_PROXY_GENERATOR = new StructProxyGenerator();
    public static final NativeCallGenerator NATIVE_CALL_GENERATOR = new NativeCallGenerator(STRUCT_PROXY_GENERATOR);

    public static final Libc LIBC = NATIVE_CALL_GENERATOR.generate(Libc.class);

    static {
        NATIVE_CALL_GENERATOR.indyMode();
    }


}
