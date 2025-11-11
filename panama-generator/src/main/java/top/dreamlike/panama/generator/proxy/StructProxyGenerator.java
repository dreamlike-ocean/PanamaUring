package top.dreamlike.panama.generator.proxy;


import top.dreamlike.panama.generator.annotation.*;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.*;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.*;
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static top.dreamlike.panama.generator.helper.NativeGeneratorHelper.REINTERPRET;
import static top.dreamlike.panama.generator.helper.NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH;
import static top.dreamlike.panama.generator.proxy.NativeLookup.primitiveMapToMemoryLayout;

public class StructProxyGenerator {
    static final String MEMORY_FIELD = "_realMemory";

    static final String GENERATOR_FIELD = "_generator";

    static final String LAYOUT_FIELD = "_layout";
    static final String CTOR_FACTORY_NAME = "_ctorFactory";
    static final String SHORTCUT_FACTORY_NAME = "_shortcut_ctor_factory";
    static final MethodHandle ENHANCE_MH;
    private static final Method REALMEMORY_METHOD;

    private static final Method GENERATOR_VARHANDLE;

    private static final Method SHORTCUT_INDY_BOOTSTRAP_METHOD;
    private static final Set<VarHandle.AccessMode> getterModes = Set.of(VarHandle.AccessMode.GET, VarHandle.AccessMode.GET_VOLATILE, VarHandle.AccessMode.GET_OPAQUE, VarHandle.AccessMode.GET_ACQUIRE);
    private static final Set<VarHandle.AccessMode> setterModes = Set.of(VarHandle.AccessMode.SET, VarHandle.AccessMode.SET_VOLATILE, VarHandle.AccessMode.SET_OPAQUE, VarHandle.AccessMode.SET_RELEASE);

    static {
        try {
            ENHANCE_MH = MethodHandles.lookup().findVirtual(StructProxyGenerator.class, "enhance", MethodType.methodType(Object.class, Class.class, MemorySegment.class));
            REALMEMORY_METHOD = NativeStructEnhanceMark.class.getMethod("realMemory");
            GENERATOR_VARHANDLE = StructProxyGenerator.class.getMethod("generateVarHandle", MemoryLayout.class, String.class);
            SHORTCUT_INDY_BOOTSTRAP_METHOD = InvokeDynamicFactory.class.getMethod("shortcutIndyFactory", MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, NativeStructProxyCtorMeta> ctorCaches = new ConcurrentHashMap<>();
    private final Map<Class<?>, MemoryLayout> layoutCaches = new ConcurrentHashMap<>();
    BiConsumer<String, byte[]> classDataPeek;
    boolean skipInit = false;
    private final ClassFile classFile = ClassFile.of();

    public StructProxyGenerator() {
    }

    private static String upperFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static VarHandle generateVarHandle(MemoryLayout current, String name) {
        VarHandle handle = current.varHandle(MemoryLayout.PathElement.groupElement(name));
        handle = MethodHandles.insertCoordinates(handle, 1, 0);
        return handle.withInvokeExactBehavior();
    }

    public static MemorySegment findMemorySegment(Object o) {
        if (o instanceof NativeStructEnhanceMark mark) {
            return mark.realMemory();
        }

        if (o instanceof NativeAddressable addressable) {
            return addressable.address();
        }

        throw new StructException("before findMemorySegment, you should enhance it");
    }

    public static boolean isNativeStruct(Object o) {
        return o instanceof NativeStructEnhanceMark || o instanceof NativeAddressable;
    }

    public static void rebind(Object proxyObject, MemorySegment memorySegment) {
        if (proxyObject instanceof NativeStructEnhanceMark memoryHolder) {
            memoryHolder.rebind(memorySegment);
            return;
        }
        throw new StructException("before rebinding, you should enhance it");
    }

    static String generateProxyClassName(Class targetClass) {
        return targetClass.getName() + "_native_struct_proxy";
    }

    static String generateVarHandleName(Field field) {
        return field.getName() + "_native_struct_vh";
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

    @SuppressWarnings("unchecked")
    public <T> T enhance(Class<T> t, MemorySegment binder) {
        Objects.requireNonNull(t);
        if (t.isPrimitive()) {
            throw new IllegalArgumentException("only support not primitive type!");
        }

        if (binder.address() == MemorySegment.NULL.address()) {
            return null;
        }

        try {
            return (T) ctorCaches.computeIfAbsent(t, this::enhance)
                    .lmfCtor()
                    .apply(binder);
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
    }

    MethodHandle findEnhancedCtor(Class<?> t) {
        return ctorCaches.computeIfAbsent(t, this::enhance)
                .mhCtor();
    }

    public <T> T allocate(SegmentAllocator allocator, Class<T> t) {
        return enhance(t, allocator.allocate(extract(t)));
    }

    public void setProxySavePath(String proxySavePath) {
        this.classDataPeek = (className, classData) -> {
            try {
                Path dir = Path.of(proxySavePath);
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
                Files.write(Path.of(proxySavePath, className + ".class"), classData, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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
        if (structClass == void.class) {
            return null;
        }
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
            throw new StructException("alignment cant be " + alignment.byteSize());
        }
        var alignmentByteSize = alignment == null ? -1 : alignment.byteSize();
        for (Field field : structClass.getDeclaredFields()) {
            if (needSkip(field)) {
                continue;
            }
            //类型为原语
            if (field.getType().isPrimitive()) {
                MemoryLayout layout = primitiveMapToMemoryLayout(field.getType()).withName(field.getName());
                layout = alignmentByteSize == -1 ? layout : layout.withByteAlignment(alignmentByteSize);
                list.add(layout);
                continue;
            }
            if (field.getAnnotation(Pointer.class) != null || field.getType() == NativeArrayPointer.class) {
                AddressLayout addressLayout = Optional.ofNullable(field.getAnnotation(Pointer.class))
                        .map(Pointer::targetLayout)
                        .filter(c -> !c.equals(void.class))
                        .or(() -> field.getType() == NativeArrayPointer.class || MemorySegment.class.isAssignableFrom(field.getType()) ? Optional.empty() : Optional.of(field.getType()))
                        .map(this::extract)
                        .map(targetLayout -> ValueLayout.ADDRESS.withName(field.getName()).withTargetLayout(targetLayout))
                        .orElse(ValueLayout.ADDRESS.withName(field.getName()));

                list.add(addressLayout);
                continue;
            }
            //类型为数组or指针
            if (field.getType() == MemorySegment.class || field.getType() == NativeArray.class) {
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
                    throw new StructException(field + " must be pointer or nativeArray");
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

    <T> NativeStructProxyCtorMeta enhance(Class<T> targetClass) {
        try {
            MemoryLayout structMemoryLayout = extract(targetClass);
            NativeGeneratorHelper.STRUCT_CONTEXT.set(new StructProxyContext(this, structMemoryLayout));
            String className = generateProxyClassName(targetClass);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
            Class<?> aClass = null;
            try {
                aClass = lookup.findClass(className);
            } catch (ClassNotFoundException ignore) {
            }
            if (aClass == null) {
                var thisClassDesc = ClassDesc.of(className);
                byte[] classByteCode = classFile.build(thisClassDesc, classBuilder -> {
                    generatorCtor(classBuilder, thisClassDesc, targetClass, structMemoryLayout);
                    ArrayList<Consumer<CodeBuilder>> clinitBlocks = new ArrayList<>();
                    Consumer<CodeBuilder> interfaceClinit = implementStructMarkInterface(classBuilder, thisClassDesc);
                    clinitBlocks.add(interfaceClinit);
                    for (Field field : targetClass.getDeclaredFields()) {
                        if (needSkip(field)) {
                            continue;
                        }
                        var clinitBlock = switch (field.getType()) {
                            case Class c when c.isPrimitive() ->
                                    generatePrimitiveFieldVarHandle(classBuilder, thisClassDesc, field, structMemoryLayout);
                            case Class c when c.equals(NativeArray.class) ->
                                    generateNativeArray(classBuilder, thisClassDesc, field, structMemoryLayout);
                            case Class c when MemorySegment.class.isAssignableFrom(c) ->
                                    generateMemorySegmentField(classBuilder, thisClassDesc, field, structMemoryLayout);
                            default -> generatorSubStructField(classBuilder, thisClassDesc, field, structMemoryLayout);
                        };
                        clinitBlocks.add(clinitBlock);
                    }

                    classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, (AccessFlag.STATIC.mask()), it -> {
                        clinitBlocks.forEach(init -> init.accept(it));
                        it.return_();
                    });
                    generateCtor(classBuilder, thisClassDesc);
                });

                if (classDataPeek != null) {
                    classDataPeek.accept(className, classByteCode);
                }

                aClass = lookup.defineClass(classByteCode);
            }

            //强制初始化执行cInit
            if (!skipInit) {
                lookup.ensureInitialized(aClass);
            }
            MethodHandle ctorMh = MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(void.class, MemorySegment.class));
            Function<MemorySegment, Object> ctorlmf = (Function<MemorySegment, Object>) aClass.getDeclaredMethod(CTOR_FACTORY_NAME).invoke(null);
            return new NativeStructProxyCtorMeta(ctorlmf, ctorMh);
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            NativeGeneratorHelper.STRUCT_CONTEXT.remove();
        }
    }

    public <T> void register(Class<T> target) {
        enhance(target);
    }

    private void generateCtor(ClassBuilder thisClass, ClassDesc thisClassDesc) {
        MethodTypeDesc methodTypeDesc = MethodTypeDesc.of(Function.class.describeConstable().get());
        thisClass.withMethodBody(CTOR_FACTORY_NAME, methodTypeDesc, AccessFlag.STATIC.mask() | AccessFlag.PUBLIC.mask(), it -> {
            it.invokedynamic(
                    DynamicCallSiteDesc.of(
                            ConstantDescs.ofCallsiteBootstrap(
                                    ClassFileHelper.toDesc(LambdaMetafactory.class), "metafactory", ConstantDescs.CD_CallSite,
                                    MethodType.class.describeConstable().get(), MethodHandle.class.describeConstable().get(), MethodType.class.describeConstable().get()
                            ),
                            "apply",
                            MethodTypeDesc.of(Function.class.describeConstable().get())
                    ).withArgs(
                            MethodTypeDesc.of(Object.class.describeConstable().get(), Object.class.describeConstable().get()),
                            MethodHandleDesc.ofConstructor(thisClassDesc, MemorySegment.class.describeConstable().get()),
                            MethodTypeDesc.of(thisClassDesc, MemorySegment.class.describeConstable().get())
                    )
            );
            it.areturn();
        });
    }
    public <T> Supplier<T> generateShortcut(Class<T> shortcutInterface) {
        if (!shortcutInterface.isInterface()) {
            throw new IllegalArgumentException("shortcut should be interface!");
        }
        String proxyName = generatorShortcutProxyName(shortcutInterface);
        NativeGeneratorHelper.STRUCT_CONTEXT.set(new StructProxyContext(this, null));
        try {
            Class<T> targetProxyClass = null;
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(shortcutInterface, MethodHandles.lookup());
            try {
                targetProxyClass = (Class<T>) lookup.findClass(proxyName);
            } catch (ClassNotFoundException ignore) {
                targetProxyClass = generateShortcutClass(lookup, shortcutInterface);
            }

            if (!skipInit) {
                lookup.ensureInitialized(targetProxyClass);
            }
            return (Supplier<T>) (targetProxyClass.getDeclaredMethod(SHORTCUT_FACTORY_NAME).invoke(null));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            NativeGeneratorHelper.STRUCT_CONTEXT.remove();
        }

    }

    private void generateShortcutCtorFactory(ClassBuilder thisClass, ClassDesc thisClassDesc) {
        MethodTypeDesc methodTypeDesc = MethodTypeDesc.of(Function.class.describeConstable().get());
        thisClass.withMethodBody(SHORTCUT_FACTORY_NAME, methodTypeDesc, AccessFlag.STATIC.mask() | AccessFlag.PUBLIC.mask(), it -> {
            it.invokedynamic(
                    DynamicCallSiteDesc.of(
                            ConstantDescs.ofCallsiteBootstrap(
                                    ClassFileHelper.toDesc(LambdaMetafactory.class), "metafactory", ConstantDescs.CD_CallSite,
                                    MethodType.class.describeConstable().get(), MethodHandle.class.describeConstable().get(), MethodType.class.describeConstable().get()
                            ),
                            "get",
                            MethodTypeDesc.of(Supplier.class.describeConstable().get())
                    ).withArgs(
                            MethodTypeDesc.of(Object.class.describeConstable().get()),
                            MethodHandleDesc.ofConstructor(thisClassDesc),
                            MethodTypeDesc.of(thisClassDesc)
                    )
            );
            it.areturn();
        });
    }

    private <T> Class<T> generateShortcutClass(MethodHandles.Lookup lookup, Class<T> interfaceClass) throws IllegalAccessException {
        List<ShortCutInfo> shortCutInfoList = Arrays.stream(interfaceClass.getMethods())
                .filter(m -> !m.isSynthetic() && !m.isBridge() && !m.isDefault())
                .map(this::parseShortcutMethod)
                .toList();

        String shortcutProxyName = generatorShortcutProxyName(interfaceClass);
        ClassDesc thisClass = ClassDesc.of(shortcutProxyName);
        var byteCode = classFile.build(thisClass, cb -> {
            cb.withField(GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class), ClassFile.ACC_STATIC | ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL);
            cb.withInterfaceSymbols(ClassFileHelper.toDesc(interfaceClass));
            for (ShortCutInfo info : shortCutInfoList) {
                Method method = info.method;
                cb.withMethodBody(method.getName(), ClassFileHelper.toMethodDescriptor(method), ClassFile.ACC_PUBLIC, it -> {
                    ClassFileHelper.loadAllArgs(method, it);
                    it.invokedynamic(
                            DynamicCallSiteDesc.of(
                                    MethodHandleDesc.ofMethod(
                                            DirectMethodHandleDesc.Kind.STATIC, ClassFileHelper.toDesc(InvokeDynamicFactory.class), SHORTCUT_INDY_BOOTSTRAP_METHOD.getName(),
                                            ClassFileHelper.toMethodDescriptor(SHORTCUT_INDY_BOOTSTRAP_METHOD)
                                    ),
                                    method.getName(),
                                    ClassFileHelper.toMethodDescriptor(method)
                            )
                    );
                    ClassFileHelper.returnValue(it, method.getReturnType());
                });
            }
            cb.withMethodBody(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, (AccessFlag.STATIC.mask()), it -> {
                ClassFileHelper.invoke(it, NativeGeneratorHelper.FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR);
                it.putstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
                it.return_();
            });

            cb.withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, ClassFile.ACC_PUBLIC, it -> {
                it.aload(0);
                it.invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void);
                it.return_();
            });
            generateShortcutCtorFactory(cb, thisClass);
        });

        if (classDataPeek != null) {
            classDataPeek.accept(shortcutProxyName, byteCode);
        }

        return (Class<T>) lookup.defineClass(byteCode);
    }

    private ShortCutInfo parseShortcutMethod(Method shortcutMethod) {
        ShortcutOption option = shortcutMethod.getAnnotation(ShortcutOption.class);
        if (option == null) {
            throw new IllegalArgumentException("shortcut method should be marked as ShortcutOption");
        }
        Class owner = option.owner();
        if (primitiveMapToMemoryLayout(owner) != null) {
            throw new IllegalArgumentException("owner cant be primitive!");
        }

        VarHandle.AccessMode accessMode = option.mode();

        if (!(getterModes.contains(accessMode) || setterModes.contains(accessMode))) {
            throw new IllegalArgumentException("mode should be getter or setter");
        }

        boolean isGetter = getterModes.contains(accessMode);
        Parameter[] parameters = shortcutMethod.getParameters();
        MemoryLayout targetFieldLayout;
        if (isGetter) {
            //需要返回值为基础类型且入参长度唯一 第一个参数类型为owner一致或者为MemorySegment
            if (parameters.length != 1 || !(parameters[0].getType().equals(owner) || MemorySegment.class.isAssignableFrom(parameters[0].getType()))) {
                throw new IllegalArgumentException("getter shortcut method should have one parameter and type should be `owner` or MemorySegment");
            }
            if (!shortcutMethod.getReturnType().isPrimitive() || shortcutMethod.getReturnType() == void.class) {
                throw new IllegalArgumentException("getter shortcut method should return primitive type");
            }
            targetFieldLayout = primitiveMapToMemoryLayout(shortcutMethod.getReturnType());
        } else {
            //setter
            //返回值为void 入参长度为2 第一个参数类型为owner一致或者为MemorySegment 第二个参数类型为owner一致
            if (parameters.length != 2 || !(parameters[0].getType().equals(owner) || MemorySegment.class.isAssignableFrom(parameters[0].getType())) || !parameters[1].getType().isPrimitive()) {
                throw new IllegalArgumentException("setter shortcut method should have two parameter and type should be `owner` or MemorySegment");
            }
            if (!shortcutMethod.getReturnType().equals(void.class)) {
                throw new IllegalArgumentException("setter shortcut method should return void");
            }
            targetFieldLayout = primitiveMapToMemoryLayout(parameters[1].getType());
        }

        //检查路径是否正确
        MemoryLayout memoryLayout = extract(owner);
        String[] pathNodes = option.value();

        if (pathNodes.length == 0) {
            throw new IllegalArgumentException("path length cant equal zero");
        }

        for (String node : pathNodes) {
            MemoryLayout fieldLayout = memoryLayout.select(MemoryLayout.PathElement.groupElement(node));
            if (fieldLayout instanceof AddressLayout addressLayout) {
                memoryLayout = addressLayout.targetLayout().orElseThrow(() -> new StructException(node + " as address layout should have target layout"));
            } else {
                memoryLayout = fieldLayout;
            }
        }
        if (memoryLayout.byteSize() != targetFieldLayout.byteSize()) {
            throw new IllegalArgumentException("shortcut type size not match");
        }

        return new ShortCutInfo(pathNodes, accessMode, shortcutMethod);
    }

    MethodHandle generateShortcutTrustedMH(Method method) {
        ShortcutOption shortcutOption = method.getAnnotation(ShortcutOption.class);
        VarHandle.AccessMode accessMode = shortcutOption.mode();

        Class ownered = shortcutOption.owner();
        MemoryLayout ownerLayout = extract(ownered);
        MemoryLayout memoryLayout = ownerLayout;
        String[] pathNodes = shortcutOption.value();
        ArrayList<MemoryLayout.PathElement> pathElements = new ArrayList<>(pathNodes.length);
        for (String node : pathNodes) {
            memoryLayout = memoryLayout.select(MemoryLayout.PathElement.groupElement(node));
            pathElements.add(MemoryLayout.PathElement.groupElement(node));
            if (memoryLayout instanceof AddressLayout addressLayout) {
                memoryLayout = addressLayout.targetLayout().get();
                pathElements.add(MemoryLayout.PathElement.dereferenceElement());
            }
        }
        MemoryLayout.PathElement[] path = pathElements.toArray(new MemoryLayout.PathElement[pathElements.size()]);
        var mh = MethodHandles.insertCoordinates(ownerLayout.varHandle(path), 1, 0L)
                .toMethodHandle(accessMode);
        return MethodHandles.filterArguments(
                mh,
                0,
                TRANSFORM_OBJECT_TO_STRUCT_MH.asType(TRANSFORM_OBJECT_TO_STRUCT_MH.type().changeParameterType(0, method.getParameters()[0].getType())));
    }

    <T> String generatorShortcutProxyName(Class<T> shortcutInterface) {
        return shortcutInterface.getName() + "_shortcut_proxy";
    }

    private boolean needSkip(Field field) {
        return field.isSynthetic() || Modifier.isStatic(field.getModifiers()) || field.getAnnotation(Skip.class) != null;
    }

    public VarHandle findFieldVarHandle(Field field) {
        Class<?> declaringClass = field.getDeclaringClass();
        MemoryLayout layout = layoutCaches.get(declaringClass);
        if (layout == null) {
            throw new StructException("you should enhance " + declaringClass + " first");
        }
        Class<?> type = field.getType();
        if (type.isPrimitive()
            || field.getAnnotation(Pointer.class) != null
            || Optional.ofNullable(field.getAnnotation(NativeArrayMark.class)).map(NativeArrayMark::asPointer).orElse(false)
        ) {
            return MethodHandles.insertCoordinates(layout.varHandle(MemoryLayout.PathElement.groupElement(field.getName())), 1, 0);
        }
        throw new StructException("only support primitive type or pointer type");
    }

    private Consumer<CodeBuilder> implementStructMarkInterface(ClassBuilder cb, ClassDesc thisClass) {

        cb.withField(GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class), ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
        cb.withField(LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class), ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
        cb.withField(MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class), ClassFile.ACC_PUBLIC);

        cb.withInterfaceSymbols(ClassFileHelper.toDesc(NativeStructEnhanceMark.class));
        cb.withMethodBody("fetchStructProxyGenerator", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.FETCH_STRUCT_PROXY_GENERATOR), ClassFile.ACC_PUBLIC, it -> {
            it.getstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
            it.areturn();
        });
        cb.withMethodBody("realMemory", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REAL_MEMORY), ClassFile.ACC_PUBLIC, it -> {
            it.aload(0);
            it.getfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            it.areturn();
        });

        cb.withMethodBody("rebind", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REBIND_MEMORY), ClassFile.ACC_PUBLIC, it -> {
            it.aload(1);

            it.aload(0);
            it.getfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));

            //NativeGeneratorHelper.rebindMemory(newSegment, this._realMemory)
            ClassFileHelper.invoke(it, NativeGeneratorHelper.REBIND_ASSERT_METHOD);

            it.aload(0);
            it.aload(1);
            //this._realMemory = newSegment;
            it.putfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            //return
            it.return_();
        });
        cb.withMethodBody("layout", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.LAYOUT), ClassFile.ACC_PUBLIC, it -> {
            it.getstatic(thisClass, LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class));
            it.areturn();
        });
        return it -> {
            ClassFileHelper.invoke(it, NativeGeneratorHelper.FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR);
            it.putstatic(thisClass, LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class));
            ClassFileHelper.invoke(it, NativeGeneratorHelper.FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR);
            it.putstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
        };
    }

    private void generatorCtor(ClassBuilder cb, ClassDesc thisClass, Class originClass, MemoryLayout structMemoryLayout) {
        cb.withSuperclass(ClassFileHelper.toDesc(originClass));
        cb.withMethodBody(ConstantDescs.INIT_NAME, MethodType.methodType(void.class, MemorySegment.class).describeConstable().get(), ClassFile.ACC_PUBLIC, it -> {
            //super()
            it.aload(0);
            it.invokespecial(ClassFileHelper.toDesc(originClass), ConstantDescs.INIT_NAME, ConstantDescs.MTD_void);
            // newSegment = memorySegment.reinterpret(structMemoryLayout.byteSize())
            it.aload(1);
            it.loadConstant(structMemoryLayout.byteSize());
            it.invokeinterface(ClassFileHelper.toDesc(MemorySegment.class), "reinterpret", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REINTERPRET));
            it.astore(1);
            //this._realMemory = newSegment;
            it.aload(0);
            it.aload(1);
            it.putfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            it.return_();
        });
    }

    private Consumer<CodeBuilder> initVarHandleBlock(ClassBuilder cb, ClassDesc thisClass, Field field) {
        String varHandleFieldName = generateVarHandleName(field);
        cb.withField(varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class), ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
        return (it) -> {
            it.getstatic(thisClass, LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class));
            it.ldc(field.getName());
            it.invokestatic(ClassFileHelper.toDesc(StructProxyGenerator.class), "generateVarHandle", ClassFileHelper.toMethodDescriptor(GENERATOR_VARHANDLE));
            it.putstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));
        };
    }

    private Consumer<CodeBuilder> generatePrimitiveFieldVarHandle(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout __) {

        String varHandleFieldName = field.getName() + "_native_struct_vh";
        cb.withMethodBody(
                "get" + upperFirstChar(field.getName()),
                MethodType.methodType(field.getType()).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    it.getstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));

                    //var m = this.realMemory()
                    it.aload(0);
                    ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);

                    //this.xxx_native_struct_vh.get(m)
                    MethodTypeDesc methodTypeDesc = MethodType.methodType(field.getType(), MemorySegment.class).describeConstable().get();
                    it.invokevirtual(ClassFileHelper.toDesc(VarHandle.class), "get", methodTypeDesc);

                    ClassFileHelper.returnValue(it, field.getType());
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, field.getType()).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    it.getstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));
                    it.aload(0);
                    ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);

                    TypeKind typeKind = TypeKind.from(field.getType());
                    //加载第一个参数
                    it.loadLocal(typeKind, it.parameterSlot(0));
                    MethodTypeDesc methodTypeDesc = MethodType.methodType(void.class, MemorySegment.class, field.getType()).describeConstable().get();
                    it.invokevirtual(ConstantDescs.CD_VarHandle, "set", methodTypeDesc);
                    it.return_();
                });

        return initVarHandleBlock(cb, thisClass, field);
    }

    private Consumer<CodeBuilder> generateNativeArray(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout structLayout) {
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        if (arrayMark == null) {
            throw new IllegalArgumentException(field + " must be marked as NativeArrayMark");
        }
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        long newSize = extract(arrayMark.size()).byteSize() * arrayMark.length();
        boolean asPointer = field.getAnnotation(Pointer.class) != null || arrayMark.asPointer();
        //不在生成出来的代码里面做分支 减少运行时分支判断
        cb.withMethodBody(
                "get" + upperFirstChar(field.getName()),
                MethodType.methodType(NativeArray.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    it.aload(0);
                    //MemorySegment realMemory = this.realMemory();
                    ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);
                    it.astore(1);
                    it.aload(1);
                    if (asPointer) {
                        it.getstatic(ClassFileHelper.toDesc(ValueLayout.class), "ADDRESS", ClassFileHelper.toDesc(AddressLayout.class));
                        //realMemory = realMemory.get(ValueLayout.ADDRESS, offset).reinterpret(newSize);
                        it.ldc(offset);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT, true);
                        it.ldc(newSize);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.REINTERPRET, true);
                    } else {
                        //realMemory = realMemory.asSlice(offset, newSize)
                        it.ldc(offset);
                        it.ldc(newSize);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.AS_SLICE, true);
                    }
                    it.astore(1);
                    it.new_(ClassFileHelper.toDesc(NativeArray.class));
                    it.dup();
                    it.getstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
                    it.aload(1);
                    it.ldc(ClassFileHelper.toDesc(arrayMark.size()));
                    //new NativeArray<>(this, realMemory, arrayMark.size());
                    it.invokespecial(
                            ClassFileHelper.toDesc(NativeArray.class),
                            ConstantDescs.INIT_NAME,
                            MethodType.methodType(void.class, StructProxyGenerator.class, MemorySegment.class, Class.class).describeConstable().get()
                    );
                    it.areturn();
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, NativeArray.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    if (asPointer) {
                        generateSetPtr(it, offset);
                    } else {
                        generateSetSubElement(it, offset, newSize);
                    }
                    it.return_();
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, MemorySegment.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    if (asPointer) {
                        generateSetPtr(it, offset);
                    } else {
                        generateSetSubElement(it, offset, newSize);
                    }
                    it.return_();
                });

        return (it) -> {
        };
    }

    private void generateSetPtr(CodeBuilder it, long offset) {
        it.aload(0);
        it.ldc(offset);
        it.aload(1);
        ClassFileHelper.invoke(it, NativeGeneratorHelper.SET_PTR);
    }

    private void generateSetSubElement(CodeBuilder it, long offset, long newSize) {
        it.aload(0);
        it.ldc(offset);
        it.ldc(newSize);
        it.aload(1);
        ClassFileHelper.invoke(it, NativeGeneratorHelper.OVER_WRITE_SUB_ELEMENT);
    }

    private Consumer<CodeBuilder> generateMemorySegmentField(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout structLayout) {
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        boolean pointerMarked = field.getAnnotation(Pointer.class) != null;
        boolean pointer = arrayMark != null && arrayMark.asPointer();
        cb.withMethodBody(
                "get" + upperFirstChar(field.getName()),
                MethodType.methodType(MemorySegment.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    it.aload(0);
                    //MemorySegment realMemory = this.realMemory();
                    ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);
                    it.astore(1);
                    it.aload(1);
                    if (pointerMarked) {
                        it.getstatic(ClassFileHelper.toDesc(ValueLayout.class), "ADDRESS", ClassFileHelper.toDesc(AddressLayout.class));
                        it.ldc(offset);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT, true);
                        it.areturn();
                        return;
                    }
                    MemoryLayout realSize = extract(arrayMark.size());
                    if (pointer) {
                        it.getstatic(ClassFileHelper.toDesc(ValueLayout.class), "ADDRESS", ClassFileHelper.toDesc(AddressLayout.class));
                        it.ldc(offset);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT, true);
                        it.ldc(realSize.byteSize() * arrayMark.length());
//                memory.reinterpret(realSize.byteSize() * arrayMark.length());
                        ClassFileHelper.invoke(it, REINTERPRET, true);
                        it.areturn();
                    } else {
//                realMemory.asSlice(offset, realSize.byteSize() * arrayMark.length());
                        it.ldc(offset);
                        it.ldc(realSize.byteSize() * arrayMark.length());
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.AS_SLICE, true);
                        it.areturn();
                    }
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, MemorySegment.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    if (pointer || pointerMarked) {
                        generateSetPtr(it, offset);
                    } else {
                        MemoryLayout realSize = extract(arrayMark.size());
                        generateSetSubElement(it, offset, realSize.byteSize() * arrayMark.length());
                    }
                    it.return_();
                });
        return it -> {
        };
    }

    private Consumer<CodeBuilder> generatorSubStructField(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout structLayout) {
        long subStructLayoutSize = structLayout.select(MemoryLayout.PathElement.groupElement(field.getName())).byteSize();
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        boolean isPointer = field.getAnnotation(Pointer.class) != null;
        cb.withMethodBody(
                "get" + upperFirstChar(field.getName()),
                MethodType.methodType(field.getType()).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    it.aload(0);
                    //MemorySegment realMemory = this.realMemory();
                    ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);
                    it.astore(1);
                    it.aload(1);
                    if (isPointer) {
                        it.getstatic(ClassFileHelper.toDesc(ValueLayout.class), "ADDRESS", ClassFileHelper.toDesc(AddressLayout.class));
                        it.ldc(offset);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT, true);
                        it.ldc(subStructLayoutSize);
                        ClassFileHelper.invoke(it, REINTERPRET, true);
                        it.astore(2);
                        it.getstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
                        it.ldc(ClassFileHelper.toDesc(field.getType()));
                        it.aload(2);
                    } else {
                        it.ldc(offset);
                        it.ldc(subStructLayoutSize);
                        ClassFileHelper.invoke(it, NativeGeneratorHelper.AS_SLICE, true);
                        it.astore(1);
                        it.getstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
                        it.ldc(ClassFileHelper.toDesc(field.getType()));
                        it.aload(1);
                    }
                    ClassFileHelper.invoke(it, NativeGeneratorHelper.ENHANCE);
                    it.checkcast(ClassFileHelper.toDesc(field.getType()));
                    it.areturn();
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, field.getType()).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    if (isPointer) {
                        generateSetPtr(it, offset);
                    } else {
                        generateSetSubElement(it, offset, subStructLayoutSize);
                    }
                    it.return_();
                });

        cb.withMethodBody(
                "set" + upperFirstChar(field.getName()),
                MethodType.methodType(void.class, MemorySegment.class).describeConstable().get(),
                ClassFile.ACC_PUBLIC,
                it -> {
                    if (isPointer) {
                        generateSetPtr(it, offset);
                    } else {
                        generateSetSubElement(it, offset, subStructLayoutSize);
                    }
                    it.return_();
                });

        return it -> {
        };
    }

    private record ShortCutInfo(String[] path, VarHandle.AccessMode accessMode, Method method) {
    }

    private record NativeStructProxyCtorMeta(Function<MemorySegment, Object> lmfCtor, MethodHandle mhCtor) {};

}

