package top.dreamlike.panama.genertor.proxy;


import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodVariableAccess;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.JavaConstant;
import top.dreamlike.panama.genertor.annotation.Alignment;
import top.dreamlike.panama.genertor.annotation.NativeArrayMark;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.annotation.Union;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.*;

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
import java.lang.reflect.Method;
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

    private static final ThreadLocal<StructProxyContext> STRUCT_CONTEXT = new ThreadLocal<>();

    private static final Method REALMEMORY_METHOD;

    static final MethodHandle ENHANCE_MH;

    static {
        try {
            ENHANCE_MH = MethodHandles.lookup().findVirtual(StructProxyGenerator.class, "enhance", MethodType.methodType(Object.class, Class.class, MemorySegment.class));
            REALMEMORY_METHOD = NativeStructEnhanceMark.class.getMethod("realMemory");
            NativeGeneratorHelper.fetchCurrentNativeStructGenerator = STRUCT_CONTEXT::get;
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

    public static VarHandle generateVarHandle(MemoryLayout current, String name) {
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

    public static void rebind(Object proxyObject, MemorySegment memorySegment) {
        if (proxyObject instanceof NativeStructEnhanceMark memoryHolder) {
            memoryHolder.rebind(memorySegment);
            return;
        }
        throw new StructException(STR."before rebinding, you should enhance it");
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
            return NativeGeneratorHelper.calAlignLayout(list);
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
            STRUCT_CONTEXT.set(new StructProxyContext(this, structMemoryLayout));
            String className = STR. "\{ targetClass.getName() }_native_struct_proxy" ;
            var precursor = byteBuddy.subclass(targetClass)
                    .name(className)
                    .implement(NativeStructEnhanceMark.class)
                    .defineField(MEMORY_FIELD, MemorySegment.class)
                    .defineField(GENERATOR_FIELD, StructProxyGenerator.class, Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC)
                    .defineField(LAYOUT_FIELD, MemoryLayout.class, Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC)
                    .method(named("fetchStructProxyGenerator")).intercept(FieldAccessor.ofField(GENERATOR_FIELD))
                    //这里 不能用 MethodDelegation.toField(MEMORY_FIELD) 因为会在对应字段MemorySegment里面寻找签名对得上的方法 就会找到asReadOnly
                    .method(named("realMemory")).intercept(FieldAccessor.ofField(MEMORY_FIELD))
                    .method(named("rebind"))
                    .intercept(
                            MethodCall.invoke(NativeGeneratorHelper.REBIND_ASSERT_METHOD).withArgument(0).withField(MEMORY_FIELD)
                                    .andThen(FieldAccessor.ofField(MEMORY_FIELD).setsArgumentAt(0))
                    )

                    .defineConstructor(Modifier.PUBLIC)
                    .withParameters(MemorySegment.class)
                    .intercept(
                            MethodCall.invoke(targetClass.getDeclaredConstructor()).onSuper()
                                    .andThen(FieldAccessor.ofField(MEMORY_FIELD).setsArgumentAt(0))
                    );

            precursor = precursor.method(named("sizeof")).intercept(FixedValue.value(structMemoryLayout.byteSize()));
            precursor = precursor.method(named("layout")).intercept(FieldAccessor.ofField(LAYOUT_FIELD));
            Implementation.Composable cInitBlock = MethodCall.invoke(NativeGeneratorHelper.FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR).setsField(named(LAYOUT_FIELD))
                    .andThen(MethodCall.invoke(NativeGeneratorHelper.FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR).setsField(named(GENERATOR_FIELD)));

            for (Field field : targetClass.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }

                if (primitiveMapToMemoryLayout(field.getType()) != null) {
                    String varHandleFieldName = STR. "\{ field.getName() }_native_struct_vh" ;
                    cInitBlock = cInitBlock.andThen(
                            MethodCall.invoke(StructProxyGenerator.class.getMethod("generateVarHandle", MemoryLayout.class, String.class))
                                    .withField(LAYOUT_FIELD).with(field.getName()).setsField(named(varHandleFieldName))
                    );
                    precursor = precursor
                            .defineField(varHandleFieldName, VarHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                            //转到对应的调用
                            .defineMethod(STR. "get\{ upperFirstChar(field.getName()) }" , field.getType(), Modifier.PUBLIC)
                            .intercept(new Implementation.Simple(new MemorySegemntVarHandleGetterStackManipulation(varHandleFieldName, className, field.getType())))

                            .defineMethod(STR. "set\{ upperFirstChar(field.getName()) }" , void.class, Modifier.PUBLIC)
                            .withParameters(field.getType())
                            .intercept(new Implementation.Simple(new MemorySegemntVarHandleSetterStackManipulation(varHandleFieldName, className, field.getType())));
                } else {
                    long offset = structMemoryLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
                    precursor = precursor
                            .defineMethod(STR. "get\{ upperFirstChar(field.getName()) }" , field.getType(), Modifier.PUBLIC)
                            .intercept(InvocationHandlerAdapter.of(new StructFieldInvocationHandle<>(this, field.getType(), offset)));
                }

            }
            precursor = precursor.invokable(MethodDescription::isTypeInitializer).intercept(cInitBlock);
            DynamicType.Unloaded<?> unloaded = precursor.make();

            if (proxySavePath != null) {
                unloaded.saveIn(new File(proxySavePath));
            }

            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
            DynamicType.Loaded<?> load = unloaded.load(targetClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup));
            Class<?> loaded = load.getLoaded();
            //强制初始化执行cInit
            lookup.ensureInitialized(loaded);
            MethodHandle ctorMh = MethodHandles.lookup().findConstructor(loaded, MethodType.methodType(void.class, MemorySegment.class));
            return NativeGeneratorHelper.memoryBinder(ctorMh, structMemoryLayout);
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            STRUCT_CONTEXT.remove();
        }
    }

    private static class MemorySegemntVarHandleGetterStackManipulation implements StackManipulation {

        private final JavaConstant.MethodType getMethodType;

        private final String fieldName;

        private final String className;

        private final Class returnClass;

        public MemorySegemntVarHandleGetterStackManipulation(String fieldName, String className, Class returnClass) {
            this.getMethodType = JavaConstant.MethodType.of(returnClass, MemorySegment.class);
            this.fieldName = fieldName;
            this.className = className;
            this.returnClass = returnClass;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size res = Size.ZERO;
            //获取静态字段压栈
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className.replace(".", "/"), fieldName, "Ljava/lang/invoke/VarHandle;");
            Size vistStaticSize = new Size(1, 1);
            res = res.aggregate(vistStaticSize);
            //
            Size callVarHandleSize = new Compound(
                    //ALOAD 0
                    MethodVariableAccess.loadThis(),
                    // INVOKEVIRTUAL top/dreamlike/panama/genertor/proxy/${currentClass}.realMemory ()Ljava/lang/foreign/MemorySegment;
                    MethodInvocation.invoke(new MethodDescription.InDefinedShape.ForLoadedMethod(REALMEMORY_METHOD)),
                    //INVOKEVIRTUAL java/lang/invoke/VarHandle.get (Ljava/lang/foreign/MemorySegment;)I
                    new VarHandlerInvocation(getMethodType, true),
                    //IRETURN
                    NativeCallGenerator.calMethodReturn(returnClass)
            ).apply(methodVisitor, implementationContext);
            res = res.aggregate(callVarHandleSize);
            return res;
        }
    }

    private static class MemorySegemntVarHandleSetterStackManipulation implements StackManipulation {
        private final JavaConstant.MethodType getMethodType;

        private final String fieldName;

        private final String className;

        private final Class paramClass;


        public MemorySegemntVarHandleSetterStackManipulation(String fieldName, String className, Class paramClass) {
            this.getMethodType = JavaConstant.MethodType.of(void.class, MemorySegment.class, paramClass);
            this.fieldName = fieldName;
            this.className = className;
            this.paramClass = paramClass;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size res = Size.ZERO;
            //获取静态字段压栈
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className.replace(".", "/"), fieldName, "Ljava/lang/invoke/VarHandle;");
            Size vistStaticSize = new Size(1, 1);
            res = res.aggregate(vistStaticSize);
            Size callVarHandleSize = new Compound(
                    //ALOAD 0
                    MethodVariableAccess.loadThis(),
                    // INVOKEVIRTUAL top/dreamlike/panama/genertor/proxy/${currentClass}.realMemory ()Ljava/lang/foreign/MemorySegment;
                    MethodInvocation.invoke(new MethodDescription.InDefinedShape.ForLoadedMethod(REALMEMORY_METHOD)),
                    // ILOAD 1
                    MethodVariableAccessLoader.calLoader(paramClass, 1).loadOp(),
                    //INVOKEVIRTUAL java/lang/invoke/VarHandle.get (Ljava/lang/foreign/MemorySegment;)I
                    new VarHandlerInvocation(getMethodType, false),
                    //RETURN
                    NativeCallGenerator.calMethodReturn(void.class)
            ).apply(methodVisitor, implementationContext);
            res = res.aggregate(callVarHandleSize);
            return res;
        }
    }


}
