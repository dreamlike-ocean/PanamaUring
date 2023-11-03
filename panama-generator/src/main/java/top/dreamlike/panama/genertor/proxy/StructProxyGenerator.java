package top.dreamlike.panama.genertor.proxy;


import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import top.dreamlike.panama.genertor.annotation.Alignment;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.annotation.Union;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;
import top.dreamlike.panama.genertor.helper.StructHelper;

import java.io.File;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class StructProxyGenerator {

    private static final String MEMORY_FIELD = "_realMemory";
    private Map<Class<?>, MethodHandleInvocationHandle> ctorCaches = new ConcurrentHashMap<>();

    private ByteBuddy byteBuddy;

    public StructProxyGenerator() {
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
    }

    public <T> T enhance(T t, MemorySegment binder) {
        Objects.requireNonNull(t);
        try {
            return (T) ctorCaches.computeIfAbsent(t.getClass(), key -> enhance(t))
                    .target().invoke(binder);
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }

    }

    private MethodHandleInvocationHandle enhance(Object t) {
        try {

            var precursor = byteBuddy.subclass(t.getClass())
                    .implement(NativeStructEnhanceMark.class)
                    .defineField(MEMORY_FIELD, MemorySegment.class)
                    .method(named("fetchStructProxyGenerator")).intercept(FixedValue.value(this))
                    //这里 不能用 MethodDelegation.toField(MEMORY_FIELD) 因为会在对应字段MemorySegment里面寻找签名对得上的方法 就会找到asReadOnly
//                    .method(named("realMemory")).intercept(MethodDelegation.toField(MEMORY_FIELD))
                    .method(named("realMemory")).intercept(FieldAccessor.ofField(MEMORY_FIELD))

                    .defineConstructor(Modifier.PUBLIC).withParameters(MemorySegment.class).intercept(
                            MethodCall.invoke(t.getClass().getDeclaredConstructor()).onSuper()
                                    .andThen(FieldAccessor.ofField(MEMORY_FIELD).setsArgumentAt(0))
                    );
            DynamicType.Unloaded<?> unloaded = precursor.make();

            unloaded.saveIn(new File("structProxy"));

            DynamicType.Loaded<?> load = unloaded.load(t.getClass().getClassLoader());
            Class<?> loaded = load.getLoaded();
            return new MethodHandleInvocationHandle(MethodHandles.lookup().findConstructor(loaded, MethodType.methodType(void.class, MemorySegment.class)));
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        }
    }

    public <T> MemoryLayout extract(Class<T> structClass) {
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
            if (field.getType().isPrimitive() || isUnionField(field)) {
                MemoryLayout layout = extract0(field).withName(field.getName());
                layout = alignmentByteSize == -1 || isUnionField(field) ? layout : layout.withByteAlignment(alignmentByteSize);
                list.add(layout);
                continue;
            }
            //todo 如何确保一定是可解析的？

            MemoryLayout layout = extract(field.getType()).withName(field.getName());
            layout = alignmentByteSize == -1 ? layout : layout.withByteAlignment(alignmentByteSize);
            list.add(layout);
        }
        return StructHelper.calAlignLayout(list);
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
        var layout = switch (source) {
            case Class c when c == int.class -> ValueLayout.JAVA_INT;
            case Class c when c == long.class -> ValueLayout.JAVA_LONG;
            case Class c when c == double.class -> ValueLayout.JAVA_DOUBLE;
            case Class c when c == float.class -> ValueLayout.JAVA_FLOAT;
            case Class c when c == byte.class -> ValueLayout.JAVA_BYTE;
            case Class c when c == boolean.class -> ValueLayout.JAVA_BOOLEAN;
            case Class c when c == char.class -> ValueLayout.JAVA_CHAR;
            case Class c when c == short.class -> ValueLayout.JAVA_SHORT;
            default -> {
                if (!isUnionField(field)) {
                    throw new StructException("Unexpected value: " + source);
                }
                Alignment alignment = field.getType().getAnnotation(Alignment.class);
                var subLayout = Arrays.stream(source.getDeclaredFields())
                        .filter(Predicate.not(Field::isSynthetic))
                        .map(this::extract0)
                        .map(memoryLayout -> setAlignment(memoryLayout, alignment))
                        .toArray(MemoryLayout[]::new);
                yield MemoryLayout.unionLayout(subLayout);
            }
        };
        layout = layout.withName(field.getName());
        Alignment alignment = field.getType().getAnnotation(Alignment.class);
        if (!isUnionField(field) && alignment != null && alignment.byteSize() > 0) {
            return layout.withByteAlignment(alignment.byteSize());
        } else {
            return layout;
        }
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

    public void s(Integer obj) {
        switch (obj) {
            case -1, 1 -> System.out.println();             // Special cases
            case Integer i when i > 0 -> System.out.println();  // Positive integer cases
            // All the remaining integers
            default -> throw new IllegalStateException("Unexpected value: " + obj);
        }
    }


}
