package top.dreamlike.panama.generator.proxy;

import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;
import top.dreamlike.panama.generator.helper.DowncallContext;

import java.lang.reflect.Method;
import java.util.Objects;

public class NativeImageHelper {

    public static void initPanamaFeature(Class nativeInterface) {
        Objects.requireNonNull(nativeInterface);
        if (!nativeInterface.isInterface()) {
            throw new IllegalArgumentException(STR."\{nativeInterface} is not interface");
        }
        NativeCallGenerator generator = new NativeCallGenerator((Object) null);
        for (Method method : nativeInterface.getMethods()) {
            if (method.isBridge() || method.isDefault() || method.isSynthetic()) {
                continue;
            }
            DowncallContext downcallContext = generator.parseDowncallContext(method);
            RuntimeForeignAccess.registerForDowncall(downcallContext.fd(), downcallContext.ops());
        }
    }
}
