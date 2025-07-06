package top.dreamlike.panama.uring.helper.unsafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class TrustedLookup {

    private static final Logger log = LoggerFactory.getLogger(TrustedLookup.class);
    public static final MethodHandles.Lookup TREUSTED_LOOKUP = currentSupportedLookup();

    public static Optional<MethodHandles.Lookup> unsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            //考虑staticFieldBase被移除/不被支持的情况
            Method staticFieldBaseMH = unsafe.getClass().getMethod("staticFieldBase", Field.class);
            Object base = staticFieldBaseMH.invoke(unsafe, implLookupField);

            Method staticFieldOffsetMH = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
            long fieldOffset = (long) staticFieldOffsetMH.invoke(unsafe, implLookupField);

            return Optional.of(((MethodHandles.Lookup) unsafeClass.getDeclaredMethod("getObject", Object.class, long.class)
                    .invoke(unsafe, base, fieldOffset)));
        } catch (Exception e) {
            log.debug("unsafe is unsupported!", e);
            return Optional.empty();
        }
    }

    public static Optional<MethodHandles.Lookup> reflectionFactory() {
        try {
            Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Object reflectionFactory = reflectionFactoryClass
                    .getDeclaredMethod("getReflectionFactory").invoke(null);
            Class<MethodHandles.Lookup> lookupClass = MethodHandles.Lookup.class;

            Constructor<?> constructor = (Constructor<?>) reflectionFactoryClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class)
                    .invoke(reflectionFactory, lookupClass, lookupClass.getDeclaredConstructor(Class.class, Class.class, int.class));
            return Optional.of(((MethodHandles.Lookup) constructor.newInstance(Object.class, null, -1)));
        } catch (Exception e) {
            log.debug("reflectionFactory is unsupported!", e);
            return Optional.empty();
        }
    }

    public static Optional<MethodHandles.Lookup> panama() {
        try (Arena arena = Arena.ofConfined()) {
            JNIEnv jniEnv = new JNIEnv(arena);
            try (GlobalRef implLookup = jniEnv.GetStaticFieldByName(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP"));
            ) {
                return Optional.of(((MethodHandles.Lookup) jniEnv.jObjectToJavaObject(implLookup.ref())));
            }
        } catch (Throwable e) {
            log.debug("panama is unsupported!", e);
            return Optional.empty();
        }
    }

    public static MethodHandles.Lookup currentSupportedLookup() {
        Optional<MethodHandles.Lookup> lookupOptional = unsafe()
                .or(TrustedLookup::reflectionFactory)
                .or(TrustedLookup::panama);
        return lookupOptional.orElseThrow(() -> new IllegalStateException("no lookup provider is not supported"));
    }

}
