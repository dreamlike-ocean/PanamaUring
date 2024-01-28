package top.dreamlike.panama.generator.proxy;

import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;
import top.dreamlike.panama.generator.helper.DowncallContext;

import java.lang.foreign.FunctionDescriptor;
import java.lang.reflect.Method;
import java.util.Objects;

public class NativeImageHelper {

    public static final String PROPERTY_IMAGE_CODE_KEY = "org.graalvm.nativeimage.imagecode";
    public static final String PROPERTY_IMAGE_CODE_VALUE_BUILDTIME = "buildtime";
    public static final String PROPERTY_IMAGE_CODE_VALUE_RUNTIME = "runtime";
    public static final String PROPERTY_IMAGE_KIND_KEY = "org.graalvm.nativeimage.kind";
    public static final String PROPERTY_IMAGE_KIND_VALUE_SHARED_LIBRARY = "shared";
    public static final String PROPERTY_IMAGE_KIND_VALUE_EXECUTABLE = "executable";

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
            FunctionDescriptor desc = downcallContext.fd();
            RuntimeForeignAccess.registerForDowncall(desc, downcallContext.ops());
        }
    }

    public static boolean inImageCode() {
        return inImageBuildtimeCode() || inImageRuntimeCode();
    }

    public static boolean inImageRuntimeCode() {
        return "runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    public static boolean inImageBuildtimeCode() {
        return "buildtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    public static boolean inExecutable() {
        ensureKindAvailable();
        return "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
    }

    public static boolean isSharedLibrary() {
        ensureKindAvailable();
        return "shared".equals(System.getProperty("org.graalvm.nativeimage.kind"));
    }

    private static void ensureKindAvailable() {
        if (inImageCode() && System.getProperty("org.graalvm.nativeimage.kind") == null) {
            throw new UnsupportedOperationException("The kind of image that is built (executable or shared library) is not available yet because the relevant command line option has not been parsed yet.");
        }
    }
}
