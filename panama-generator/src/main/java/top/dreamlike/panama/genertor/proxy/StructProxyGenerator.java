package top.dreamlike.panama.genertor.proxy;


import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.*;
import top.dreamlike.panama.genertor.annotation.Alignment;
import top.dreamlike.panama.genertor.annotation.NativeArrayMark;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.annotation.Union;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.NativeHelper;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.io.File;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static top.dreamlike.panama.genertor.proxy.NativeLibLookup.primitiveMapToMemoryLayout;

public class StructProxyGenerator {

    private static final String MEMORY_FIELD = "_realMemory";

    private static final String GENERATOR_FIELD = "_generator";

    private static final String LAYOUT_FIELD = "_layout";

    String proxySavePath;

    private static final MethodHandle REINTERPRET_MH;

    private static final ThreadLocal<MemoryLayout> currentLayout = new ThreadLocal<>();

    static {
        try {
            REINTERPRET_MH = MethodHandles.lookup().findVirtual(MemorySegment.class, "reinterpret", MethodType.methodType(MemorySegment.class, long.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, Function<MemorySegment, Object>> ctorCaches = new ConcurrentHashMap<>();

    private final Map<Class<?>, MemoryLayout> layoutCaches = new ConcurrentHashMap<>();

    private final ByteBuddy byteBuddy;

    public StructProxyGenerator() {
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
    }


    private static String upperFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static VarHandle generateVarHandle(String name) {
        MemoryLayout current = currentLayout.get();
        if (current == null) {
            throw new IllegalArgumentException("must in context!");
        }
        return current.varHandle(MemoryLayout.PathElement.groupElement(name));
    }

    @SuppressWarnings("unchecked")
    public <T> T enhance(MemorySegment binder, T... dummy) {
        return this.enhance((Class<T>) dummy.getClass().componentType(), binder);
    }

    public <T> NativeArray<T> enhanceArray(MemorySegment chunk, T... dummy) {
        return new NativeArray<>(this, chunk, dummy);
    }

    public static boolean isNativeStruct(Object o) {
        return o instanceof NativeStructEnhanceMark;
    }

    @SuppressWarnings("unchecked")
    public <T> T enhance(Class<T> t, MemorySegment binder) {
        Objects.requireNonNull(t);
        try {
            return (T) ctorCaches.computeIfAbsent(t, this::enhance)
                    .apply(binder);
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
    }

    public void setProxySavePath(String proxySavePath) {
        this.proxySavePath = proxySavePath;
    }

    private MemoryLayout setAlignment(MemoryLayout memoryLayout, Alignment alignment) {
        if (alignment == null || alignment.byteSize() <= 0) {
            return memoryLayout;
        }
        return memoryLayout.withByteAlignment(alignment.byteSize());
    }

    public boolean isUnionField(Field field) {
        return field.getAnnotation(Union.class) != null || field.getType().getAnnotation(Union.class) != null;
    }

    public <T> MemoryLayout extract(Class<T> structClass) {
        return layoutCaches.computeIfAbsent(structClass, (s) -> {
            MemoryLayout mayPrimitive = primitiveMapToMemoryLayout(structClass);
            if (mayPrimitive != null) return mayPrimitive;

            ArrayList<MemoryLayout> list = new ArrayList<>();
            Alignment alignment = structClass.getAnnotation(Alignment.class);
            if (alignment != null && alignment.byteSize() <= 0) {
                throw new StructException(STR. "alignment cant be \{ alignment.byteSize() }" );
            }
            var alignmentByteSize = alignment == null ? -1 : alignment.byteSize();
            for (Field field : structClass.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                //类型为原语
                if (field.getType().isPrimitive() || isUnionField(field)) {
                    MemoryLayout layout = extract0(field).withName(field.getName());
                    layout = alignmentByteSize == -1 || isUnionField(field) ? layout : layout.withByteAlignment(alignmentByteSize);
                    list.add(layout);
                    continue;
                }

                //类型为数组or指针
                if (field.getType() == MemorySegment.class) {
                    if (field.getAnnotation(Pointer.class) != null) {
                        list.add(ValueLayout.ADDRESS.withName(field.getName()));
                        continue;
                    }
                    NativeArrayMark nativeArrayMark = field.getAnnotation(NativeArrayMark.class);
                    if (nativeArrayMark != null) {
                        SequenceLayout layout = MemoryLayout.sequenceLayout(nativeArrayMark.length(), extract(nativeArrayMark.size()));
                        list.add(layout.withName(field.getName()));
                        continue;
                    } else {
                        throw new StructException(STR. "\{ field } must be pointer or nativeArray" );
                    }
                }
                //类型为结构体
                MemoryLayout layout = extract(field.getType()).withName(field.getName());
                layout = alignmentByteSize == -1 ? layout : layout.withByteAlignment(alignmentByteSize);
                list.add(layout);
            }
            return NativeHelper.calAlignLayout(list);
        });

    }

    /**
     * @param field 已经递归到原子化的字段
     * @return 对应的FFI布局
     */
    private MemoryLayout extract0(Field field) {
        Class source = field.getType();
        boolean isPoint = field.getAnnotation(Pointer.class) != null;
        if (isPoint) {
            if (source == int.class || source == long.class) {
                return ValueLayout.ADDRESS.withName(field.getName());
            }
            throw new StructException(STR. "\{ field } type is \{ source },so it cant be point" );
        }
        MemoryLayout layout = primitiveMapToMemoryLayout(source);
        if (layout == null) {
            if (!isUnionField(field)) {
                throw new StructException(STR. "Unexpected value: \{ source }" );
            }
            Alignment alignment = field.getType().getAnnotation(Alignment.class);
            var subLayout = Arrays.stream(source.getDeclaredFields())
                    .filter(Predicate.not(Field::isSynthetic))
                    .map(this::extract0)
                    .map(memoryLayout -> setAlignment(memoryLayout, alignment))
                    .toArray(MemoryLayout[]::new);
            layout = MemoryLayout.unionLayout(subLayout);
        }

        layout = layout.withName(field.getName());
        Alignment alignment = field.getType().getAnnotation(Alignment.class);
        if (!isUnionField(field) && alignment != null && alignment.byteSize() > 0) {
            return layout.withByteAlignment(alignment.byteSize());
        } else {
            return layout;
        }
    }

    <T> Function<MemorySegment, Object> enhance(Class<T> targetClass) {
        try {
            MemoryLayout structMemoryLayout = extract(targetClass);
            currentLayout.set(structMemoryLayout);
            var precursor = byteBuddy.subclass(targetClass)
                    .implement(NativeStructEnhanceMark.class)
                    .defineField(MEMORY_FIELD, MemorySegment.class)
                    .defineField(GENERATOR_FIELD, StructProxyGenerator.class)
                    .defineField(LAYOUT_FIELD, MemoryLayout.class)
                    .method(named("fetchStructProxyGenerator")).intercept(FieldAccessor.ofField(GENERATOR_FIELD))
                    //这里 不能用 MethodDelegation.toField(MEMORY_FIELD) 因为会在对应字段MemorySegment里面寻找签名对得上的方法 就会找到asReadOnly
                    .method(named("realMemory")).intercept(FieldAccessor.ofField(MEMORY_FIELD))
                    .defineConstructor(Modifier.PUBLIC)
                    .withParameters(MemoryLayout.class, StructProxyGenerator.class, MemorySegment.class)
                    .intercept(
                            MethodCall.invoke(targetClass.getDeclaredConstructor()).onSuper()
                                    .andThen(FieldAccessor.ofField(LAYOUT_FIELD).setsArgumentAt(0))
                                    .andThen(FieldAccessor.ofField(GENERATOR_FIELD).setsArgumentAt(1))
                                    .andThen(FieldAccessor.ofField(MEMORY_FIELD).setsArgumentAt(2))
                    );

            precursor = precursor.method(named("sizeof")).intercept(FixedValue.value(structMemoryLayout.byteSize()));
            precursor = precursor.method(named("layout")).intercept(FieldAccessor.ofField(LAYOUT_FIELD));
            Implementation.Composable cInitBlock = MethodCall.invoke(NativeHelper.EMPTY_METHOD);

            for (Field field : targetClass.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }

                if (primitiveMapToMemoryLayout(field.getType()) != null) {
                    VarHandle nativeVarhandle = structMemoryLayout.varHandle(MemoryLayout.PathElement.groupElement(field.getName()));
                    String varHandleFieldName = STR. "\{ field.getName() }_native_struct_vh" ;
                    cInitBlock.andThen(
                            MethodCall.invoke(StructProxyGenerator.class.getMethod("generateVarHandle", String.class))
                                    .with(field.getName())
                    );
                    //todo fixme 使用varhandle调用
                    precursor = precursor
                            .defineField(varHandleFieldName, VarHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                            //转到对应的调用
                            .defineMethod(STR. "get\{ upperFirstChar(field.getName()) }" , field.getType(), Modifier.PUBLIC)
                            .intercept(InvocationHandlerAdapter.of(new DereferenceGetterInvocationHandle(nativeVarhandle)))

                            .defineMethod(STR. "set\{ upperFirstChar(field.getName()) }" , void.class, Modifier.PUBLIC)
                            .withParameters(field.getType())
                            .intercept(InvocationHandlerAdapter.of(new DereferenceSetterInvocationHandle(nativeVarhandle)));
                } else {
                    long offset = structMemoryLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
                    precursor = precursor
                            .defineMethod(STR. "get\{ upperFirstChar(field.getName()) }" , field.getType(), Modifier.PUBLIC)
                            .intercept(InvocationHandlerAdapter.of(new StructFieldInvocationHandle<>(this, field.getType(), offset)));
                }

            }

            DynamicType.Unloaded<?> unloaded = precursor.make();

            if (proxySavePath != null) {
                unloaded.saveIn(new File(proxySavePath));
            }

            DynamicType.Loaded<?> load = unloaded.load(targetClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()));
            Class<?> loaded = load.getLoaded();
            //强制初始化执行cInit
            MethodHandles.lookup().ensureInitialized(loaded);
            MethodHandle ctorMh = MethodHandles.lookup().findConstructor(loaded, MethodType.methodType(void.class, MemoryLayout.class, StructProxyGenerator.class, MemorySegment.class));
            return NativeHelper.memoryBinder(ctorMh, structMemoryLayout, this);
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            currentLayout.remove();
        }
    }
}
