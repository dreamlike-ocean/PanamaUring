package top.dreamlike.panama.genertor.proxy;


import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodCall;
import top.dreamlike.panama.genertor.annotation.Alignment;
import top.dreamlike.panama.genertor.annotation.NativeArray;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.annotation.Union;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;
import top.dreamlike.panama.genertor.helper.StructHelper;

import java.io.File;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
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
import java.util.function.Predicate;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class StructProxyGenerator {

    private static final String MEMORY_FIELD = "_realMemory";
    final Map<Class<?>, MethodHandleInvocationHandle> ctorCaches = new ConcurrentHashMap<>();

    private Map<Class<?>, MemoryLayout> layoutCaches = new ConcurrentHashMap<>();

    private ByteBuddy byteBuddy;

    public StructProxyGenerator() {
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
    }

    private static MemoryLayout primitiveMap(Class source) {
        return switch (source) {
            case Class c when c == int.class -> ValueLayout.JAVA_INT;
            case Class c when c == long.class -> ValueLayout.JAVA_LONG;
            case Class c when c == double.class -> ValueLayout.JAVA_DOUBLE;
            case Class c when c == float.class -> ValueLayout.JAVA_FLOAT;
            case Class c when c == byte.class -> ValueLayout.JAVA_BYTE;
            case Class c when c == boolean.class -> ValueLayout.JAVA_BOOLEAN;
            case Class c when c == char.class -> ValueLayout.JAVA_CHAR;
            case Class c when c == short.class -> ValueLayout.JAVA_SHORT;
            default -> null;
        };
    }

    private static String upperFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public <T> T enhance(Class<T> t, MemorySegment binder) {
        Objects.requireNonNull(t);
        try {
            return (T) ctorCaches.computeIfAbsent(t, key -> enhance(t))
                    .target().invoke(binder);
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }

    }

    private <T> MethodHandleInvocationHandle enhance(Class<T> targetClass) {
        try {

            var precursor = byteBuddy.subclass(targetClass)
                    .implement(NativeStructEnhanceMark.class)
                    .defineField(MEMORY_FIELD, MemorySegment.class)
                    .method(named("fetchStructProxyGenerator")).intercept(FixedValue.value(this))
                    //这里 不能用 MethodDelegation.toField(MEMORY_FIELD) 因为会在对应字段MemorySegment里面寻找签名对得上的方法 就会找到asReadOnly
//                    .method(named("realMemory")).intercept(MethodDelegation.toField(MEMORY_FIELD))
                    .method(named("realMemory")).intercept(FieldAccessor.ofField(MEMORY_FIELD))
                    .defineConstructor(Modifier.PUBLIC).withParameters(MemorySegment.class)
                    .intercept(
                            MethodCall.invoke(targetClass.getDeclaredConstructor()).onSuper()
                                    .andThen(FieldAccessor.ofField(MEMORY_FIELD).setsArgumentAt(0))
                    );

            MemoryLayout structMemoryLayout = extract(targetClass);
            precursor = precursor.method(named("sizeof")).intercept(FixedValue.value(structMemoryLayout.byteSize()));
            precursor = precursor.method(named("layout")).intercept(FixedValue.value(structMemoryLayout));

            for (Field field : targetClass.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }

                if (primitiveMap(field.getType()) != null) {
                    VarHandle nativeVarhandle = structMemoryLayout.varHandle(MemoryLayout.PathElement.groupElement(field.getName()));
                    precursor = precursor
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
            unloaded.saveIn(new File("structProxy"));

            DynamicType.Loaded<?> load = unloaded.load(targetClass.getClassLoader());
            Class<?> loaded = load.getLoaded();
            return new MethodHandleInvocationHandle(MethodHandles.lookup().findConstructor(loaded, MethodType.methodType(void.class, MemorySegment.class)));
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        }
    }

    private <T> MemoryLayout extract(Class<T> structClass) {
        MemoryLayout memoryLayout = layoutCaches.get(structClass);
        if (memoryLayout != null) {
            return memoryLayout;
        }
        MemoryLayout mayPrimitive = primitiveMap(structClass);
        if (mayPrimitive != null) {
            layoutCaches.put(structClass, mayPrimitive);
            return mayPrimitive;
        }
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
                NativeArray nativeArray = field.getAnnotation(NativeArray.class);
                if (nativeArray != null) {
                    SequenceLayout layout = MemoryLayout.sequenceLayout(nativeArray.length(), extract(nativeArray.size()));
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
        MemoryLayout layout = StructHelper.calAlignLayout(list);
        layoutCaches.put(structClass, layout);
        return layout;
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
        MemoryLayout layout = primitiveMap(source);
        if (layout == null) {
            if (!isUnionField(field)) {
                throw new StructException("Unexpected value: " + source);
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
}
