
package top.dreamlike.panama.generator.proxy;


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
import top.dreamlike.panama.generator.annotation.Alignment;
import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.annotation.Union;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.*;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static top.dreamlike.panama.generator.proxy.NativeLookup.primitiveMapToMemoryLayout;

public class StructProxyGenerator {
    private static final String MEMORY_FIELD = "_realMemory";

    private static final String GENERATOR_FIELD = "_generator";

    private static final String LAYOUT_FIELD = "_layout";
    Consumer<DynamicType.Unloaded> beforeGenerateCallBack;

    private static final ThreadLocal<StructProxyContext> STRUCT_CONTEXT = new ThreadLocal<>();

    private static final Method REALMEMORY_METHOD;

    static final MethodHandle ENHANCE_MH;

    private volatile boolean use_lmf = !NativeImageHelper.inExecutable();

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

    ByteBuddy byteBuddy;

    public StructProxyGenerator() {
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
    }

    StructProxyGenerator(Object workForGraal) {
    }


    private static String upperFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static VarHandle generateVarHandle(MemoryLayout current, String name) {
        return current.varHandle(MemoryLayout.PathElement.groupElement(name));
    }

    public static MemorySegment findMemorySegment(Object o) {
        if (o instanceof NativeStructEnhanceMark mark) {
            return mark.realMemory();
        }
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unchecked")
    public <T> T enhance(MemorySegment binder, T... dummy) {
        return this.enhance((Class<T>) dummy.getClass().componentType(), binder);
    }

    @SafeVarargs
    public final <T> NativeArray<T> enhanceArray(MemorySegment chunk, T... dummy) {
        Class<T> component = (Class<T>) dummy.getClass().getComponentType();
        return new NativeArray<>(this, chunk, component);
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
        this.beforeGenerateCallBack = (unloaded) -> {
            try {
                unloaded.saveIn(new File(proxySavePath));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };
    }

    private MemoryLayout setAlignment(MemoryLayout memoryLayout, Alignment alignment) {
        if (alignment == null || alignment.byteSize() <= 0) {
            return memoryLayout;
        }
        return memoryLayout.withByteAlignment(alignment.byteSize());
    }

    public boolean isUnion(Class type) {
        return type.getAnnotation(Union.class) != null;
    }

    public <T> MemoryLayout extract(Class<T> structClass) {
        MemoryLayout memoryLayout = layoutCaches.get(structClass);
        if (memoryLayout != null) {
            return memoryLayout;
        }

        MemoryLayout mayPrimitive = primitiveMapToMemoryLayout(structClass);
        if (mayPrimitive != null) {
            layoutCaches.put(structClass, mayPrimitive);
            return mayPrimitive;
        }

        ArrayList<MemoryLayout> list = new ArrayList<>();
        Alignment alignment = structClass.getAnnotation(Alignment.class);
        if (alignment != null && alignment.byteSize() <= 0) {
            throw new StructException(STR."alignment cant be \{alignment.byteSize()}");
        }
        var alignmentByteSize = alignment == null ? -1 : alignment.byteSize();
        for (Field field : structClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            //类型为原语
            if (field.getType().isPrimitive()) {
                MemoryLayout layout = primitiveMapToMemoryLayout(field.getType()).withName(field.getName());
                layout = alignmentByteSize == -1 ? layout : layout.withByteAlignment(alignmentByteSize);
                list.add(layout);
                continue;
            }
            //类型为数组or指针
            if (field.getType() == MemorySegment.class || field.getType() == NativeArray.class) {
                if (field.getAnnotation(Pointer.class) != null) {
                    list.add(ValueLayout.ADDRESS.withName(field.getName()));
                    continue;
                }
                NativeArrayMark nativeArrayMark = field.getAnnotation(NativeArrayMark.class);
                if (nativeArrayMark != null) {
                    if (nativeArrayMark.asPointer()) {
                        list.add(ValueLayout.ADDRESS.withName(field.getName()));
                    } else {
                        SequenceLayout layout = MemoryLayout.sequenceLayout(nativeArrayMark.length(), extract(nativeArrayMark.size()));
                        list.add(layout.withName(field.getName()));
                    }
                    continue;
                } else {
                    throw new StructException(STR."\{field} must be pointer or nativeArray");
                }
            }
            //类型为结构体
            MemoryLayout layout = extract(field.getType()).withName(field.getName());
            layout = alignmentByteSize == -1 ? layout : layout.withByteAlignment(alignmentByteSize);
            list.add(layout);
        }
        if (isUnion(structClass)) {
            UnionLayout unionLayout = MemoryLayout.unionLayout(list.toArray(MemoryLayout[]::new));
            layoutCaches.put(structClass, unionLayout);
            return unionLayout;
        }
        memoryLayout = NativeGeneratorHelper.calAlignLayout(list);
        layoutCaches.put(structClass, memoryLayout);
        return memoryLayout;
    }

    <T> Function<MemorySegment, Object> enhance(Class<T> targetClass) {
        try {
            MemoryLayout structMemoryLayout = extract(targetClass);
            STRUCT_CONTEXT.set(new StructProxyContext(this, structMemoryLayout));
            String className = generatorProxyClassName(targetClass);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
            Class<?> aClass = null;
            try {
                aClass = lookup.findClass(className);
            } catch (ClassNotFoundException ignore) {
            }
            if (aClass == null) {
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
                    precursor = switch (field.getType()) {
                        case Class c when primitiveMapToMemoryLayout(c) != null -> {
                            String varHandleFieldName = STR."\{field.getName()}_native_struct_vh";
                            cInitBlock = cInitBlock.andThen(
                                    MethodCall.invoke(StructProxyGenerator.class.getMethod("generateVarHandle", MemoryLayout.class, String.class))
                                            .withField(LAYOUT_FIELD).with(field.getName()).setsField(named(varHandleFieldName))
                            );
                            yield handlePrimitiveField(precursor, field, className);
                        }
                        case Class c when c.equals(NativeArray.class) ->
                                handleNativeArrayField(precursor, structMemoryLayout, field);
                        case Class c when MemorySegment.class.isAssignableFrom(c) ->
                                handleMemorySegmentField(precursor, structMemoryLayout, field);
                        default -> precursor
                                .defineMethod(STR."get\{upperFirstChar(field.getName())}", field.getType(), Modifier.PUBLIC)
                                .intercept(InvocationHandlerAdapter.of(new StructFieldInvocationHandle<>(this, field.getType(), structMemoryLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName())), field.getAnnotation(Pointer.class) != null)));
                    };

                }

                precursor = precursor.invokable(MethodDescription::isTypeInitializer).intercept(cInitBlock);
                DynamicType.Unloaded<?> unloaded = precursor.make();

                if (beforeGenerateCallBack != null) {
                    beforeGenerateCallBack.accept(unloaded);
                }

                aClass = unloaded.load(targetClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup)).getLoaded();
            }

            //强制初始化执行cInit
            lookup.ensureInitialized(aClass);


            MethodHandle ctorMh = MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(void.class, MemorySegment.class));
            if (use_lmf) {
                return NativeGeneratorHelper.memoryBinder(ctorMh, structMemoryLayout);
            }
            var ctorErased = ctorMh.asType(ctorMh.type().changeReturnType(Object.class));
            return (memorySegment) -> {
                memorySegment = memorySegment.reinterpret(structMemoryLayout.byteSize());
                try {
                    return ctorErased.invokeExact(memorySegment);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            };
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            STRUCT_CONTEXT.remove();
        }
    }

    public <T> void register(Class<T> target) {
        enhance(target);
    }


    public void useLmf(boolean use_lmf) {
        this.use_lmf = use_lmf;
    }

    String generatorProxyClassName(Class targetClass) {
        return STR."\{targetClass.getName()}_native_struct_proxy";
    }

    private <T> DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> handlePrimitiveField(DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> precursor, Field field, String className) {
        String varHandleFieldName = STR."\{field.getName()}_native_struct_vh";
        return precursor
                .defineField(varHandleFieldName, VarHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                //转到对应的调用
                .defineMethod(STR."get\{upperFirstChar(field.getName())}", field.getType(), Modifier.PUBLIC)
                .intercept(new Implementation.Simple(new MemorySegemntVarHandleGetterStackManipulation(varHandleFieldName, className, field.getType())))

                .defineMethod(STR."set\{upperFirstChar(field.getName())}", void.class, Modifier.PUBLIC)
                .withParameters(field.getType())
                .intercept(new Implementation.Simple(new MemorySegemntVarHandleSetterStackManipulation(varHandleFieldName, className, field.getType())));
    }

    private <T> DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> handleMemorySegmentField(DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> precursor, MemoryLayout structLayout, Field field) {
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        boolean pointerMarked = field.getAnnotation(Pointer.class) != null;

        return precursor
                .defineMethod(STR."get\{upperFirstChar(field.getName())}", field.getType(), Modifier.PUBLIC)
                .intercept(InvocationHandlerAdapter.of((Object proxy, Method method, Object[] args) -> {
                    MemorySegment realMemory = ((NativeStructEnhanceMark) proxy).realMemory();
                    if (pointerMarked) {
                        return realMemory.get(ValueLayout.ADDRESS, offset);
                    }
                    //extract保证这俩不会同时为空
                    MemorySegment needReturn;
                    MemoryLayout realSize = extract(arrayMark.size());
                    boolean pointer = arrayMark.asPointer();
                    if (pointer) {
                        MemorySegment memory = realMemory.get(ValueLayout.ADDRESS, offset);
                        needReturn = memory.reinterpret(realSize.byteSize() * arrayMark.length());
                    } else {
                        needReturn = realMemory.asSlice(offset, realSize.byteSize() * arrayMark.length());
                    }
                    return needReturn;
                }));
    }

    private <T> DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> handleNativeArrayField(DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<T> precursor, MemoryLayout structLayout, Field field) {

        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        if (arrayMark == null) throw new IllegalArgumentException(STR."\{field} must be marked as NativeArrayMark");
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        long newSize = extract(arrayMark.size()).byteSize() * arrayMark.length();
        boolean asPointer = field.getAnnotation(Pointer.class) != null || arrayMark.asPointer();
        return precursor
                .defineMethod(STR."get\{upperFirstChar(field.getName())}", field.getType(), Modifier.PUBLIC)
                .intercept(InvocationHandlerAdapter.of((Object proxy, Method method, Object[] args) -> {
                    MemorySegment realMemory = ((NativeStructEnhanceMark) proxy).realMemory();
                    if (asPointer) {
                        realMemory = realMemory.get(ValueLayout.ADDRESS, offset)
                                .reinterpret(newSize);
                        return new NativeArray<>(this, realMemory, arrayMark.size());
                    }
                    return new NativeArray<>(this, realMemory.asSlice(offset, newSize), arrayMark.size());
                }));
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
