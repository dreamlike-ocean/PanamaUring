package top.dreamlike.panama.genertor.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import top.dreamlike.panama.genertor.annotation.NativeFunction;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class NativeCallGenerator {
    private final ByteBuddy byteBuddy;

    private final NativeLibLookup nativeLibLookup;

    private static final MethodHandle TRANSFORM_OBJECT_TO_STRUCT_MH;

    static {
        try {
            TRANSFORM_OBJECT_TO_STRUCT_MH = MethodHandles.lookup()
                    .findStatic(NativeCallGenerator.class, "transToPoint", MethodType.methodType(MemorySegment.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, MethodHandleInvocationHandle> ctorCaches = new ConcurrentHashMap<>();
    private final StructProxyGenerator structProxyGenerator;

    public NativeCallGenerator() {
        this.structProxyGenerator = new StructProxyGenerator();
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLibLookup();
    }

    public NativeCallGenerator(StructProxyGenerator structProxyGenerator) {
        this.structProxyGenerator = structProxyGenerator;
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLibLookup();
    }

    private static MemorySegment transToPoint(Object o) {
        if (o instanceof NativeStructEnhanceMark struct) {
            return struct.realMemory();
        }
        if (o instanceof MemorySegment memorySegment) {
            return memorySegment;
        }
        throw new StructException(STR. "\{ o.getClass() } is not struct,pleace call StructProxyGenerator::enhance before calling native function" );
    }

    @SuppressWarnings("unchecked")
    public <T> T generate(Class<T> nativeInterface) {
        Objects.requireNonNull(nativeInterface);
        try {
            return (T) ctorCaches.computeIfAbsent(nativeInterface, key -> bind(nativeInterface))
                    .target().invoke();
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
    }

    private <T> MethodHandleInvocationHandle bind(Class<T> nativeInterface) {
        try {
            if (!nativeInterface.isInterface()) {
                throw new IllegalArgumentException(STR. "\{ nativeInterface } is not interface" );
            }
            var definition = byteBuddy.subclass(Object.class)
                    .implement(nativeInterface)
                    .name(STR. "\{ nativeInterface.getName() }_native_call_enhance" );
            for (Method method : nativeInterface.getMethods()) {
                if (method.isBridge() || method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                String mhFieldName = STR. "\{ method.getName() }_native_method_handle" ;
                MethodHandle methodHandle = nativeMethodHandle(method);
                definition = definition
                        .defineField(mhFieldName, MethodHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                        .defineMethod(method.getName(), method.getReturnType(), Modifier.PUBLIC)
                        .withParameters(method.getParameterTypes())
                        .intercept(InvocationHandlerAdapter.of(new MethodHandleInvocationHandle(methodHandle)));
            }
            DynamicType.Unloaded<Object> unloaded = definition.make();
            if (structProxyGenerator.proxySavePath != null) {
                unloaded.saveIn(new File(structProxyGenerator.proxySavePath));
            }

            Class<?> aClass = unloaded.load(nativeInterface.getClassLoader()).getLoaded();
            return new MethodHandleInvocationHandle(
                    MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(void.class))
            );
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        }
    }

    private MethodHandle nativeMethodHandle(Method method) {
        NativeFunction function = method.getAnnotation(NativeFunction.class);
        ArrayList<Linker.Option> options = new ArrayList<>(2);
        if (function != null && function.fast()) {
            options.add(Linker.Option.isTrivial());
        }
        boolean allowPassHeap = function != null && function.allowPassHeap();
        String functionName = function == null ? method.getName() : function.value();

        ArrayList<Integer> pointIndex = new ArrayList<>();
        MemoryLayout[] layouts = new MemoryLayout[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getType().isAssignableFrom(MemorySegment.class)
                    || parameters[i].getType().isAssignableFrom(NativeStructEnhanceMark.class)
                    || (!parameters[i].getType().isPrimitive() && parameters[i].getAnnotation(Pointer.class) != null)
            ) {
                layouts[i] = ValueLayout.ADDRESS;
                pointIndex.add(i);
                continue;
            }

            layouts[i] = structProxyGenerator.extract(parameters[i].getType());
        }
        FunctionDescriptor fd = method.getReturnType() == void.class
                ? FunctionDescriptor.ofVoid(layouts)
                : FunctionDescriptor.of(structProxyGenerator.extract(method.getReturnType()), layouts);
        MethodHandle methodHandle = nativeLibLookup.downcallHandle(functionName, fd, options.toArray(Linker.Option[]::new));
        for (Integer i : pointIndex) {
            methodHandle = MethodHandles.filterArguments(
                    methodHandle,
                    i,
                    TRANSFORM_OBJECT_TO_STRUCT_MH
            );
        }
        return methodHandle;
    }

}
