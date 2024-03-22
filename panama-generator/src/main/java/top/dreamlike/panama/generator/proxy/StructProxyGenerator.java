

package top.dreamlike.panama.generator.proxy;


import top.dreamlike.panama.generator.annotation.Alignment;
import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.annotation.Union;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.*;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static top.dreamlike.panama.generator.proxy.NativeLookup.primitiveMapToMemoryLayout;

public class StructProxyGenerator {
    static final String MEMORY_FIELD = "_realMemory";

    static final String GENERATOR_FIELD = "_generator";

    static final String LAYOUT_FIELD = "_layout";
    BiConsumer<String, byte[]> classDataPeek;

    private static final Method REALMEMORY_METHOD;

    private static final Method GENERATOR_VARHANDLE;

    static final MethodHandle ENHANCE_MH;

    private volatile boolean use_lmf = !NativeImageHelper.inExecutable();

    private ClassFile classFile = ClassFile.of();

    static {
        try {
            ENHANCE_MH = MethodHandles.lookup().findVirtual(StructProxyGenerator.class, "enhance", MethodType.methodType(Object.class, Class.class, MemorySegment.class));
            REALMEMORY_METHOD = NativeStructEnhanceMark.class.getMethod("realMemory");
            GENERATOR_VARHANDLE = StructProxyGenerator.class.getMethod("generateVarHandle", MemoryLayout.class, String.class);
//            NativeGeneratorHelper.fetchCurrentNativeStructGenerator = STRUCT_CONTEXT::get;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, Function<MemorySegment, Object>> ctorCaches = new ConcurrentHashMap<>();

    private final Map<Class<?>, MemoryLayout> layoutCaches = new ConcurrentHashMap<>();

    public StructProxyGenerator() {
    }

    private static String upperFirstChar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static VarHandle generateVarHandle(MemoryLayout current, String name) {
        VarHandle handle = current.varHandle(MemoryLayout.PathElement.groupElement(name));
        handle = MethodHandles.insertCoordinates(handle, 1, 0);
        return handle;
    }

    public static MemorySegment findMemorySegment(Object o) {
        if (o instanceof NativeStructEnhanceMark mark) {
            return mark.realMemory();
        }
        throw new StructException("before findMemorySegment, you should enhance it");
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
        return o instanceof NativeStructEnhanceMark || o instanceof NativeAddressable;
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
        if (t.isPrimitive()) {
            throw new IllegalArgumentException("only support not primitive type!");
        }

        if (binder.address() == MemorySegment.NULL.address()) {
            return null;
        }

        try {
            return (T) ctorCaches.computeIfAbsent(t, this::enhance)
                    .apply(binder);
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
    }

    public void setProxySavePath(String proxySavePath) {
        this.classDataPeek = (className, classData) -> {
            try {
                Path dir = Path.of(proxySavePath);
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
                Files.write(Path.of(proxySavePath, STR."\{className}.class"), classData, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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
            throw new StructException(STR."alignment cant be \{alignment.byteSize()}");
        }
        var alignmentByteSize = alignment == null ? -1 : alignment.byteSize();
        for (Field field : structClass.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
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
                        .or(() -> Optional.of(field.getType()))
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
            NativeGeneratorHelper.STRUCT_CONTEXT.set(new StructProxyContext(this, structMemoryLayout));
            String className = generateProxyClassName(targetClass);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
            Class<?> aClass = null;
            try {
                aClass = lookup.findClass(className);
            } catch (ClassNotFoundException ignore) {
            }
            if (aClass == null) {
                var thisClassDesc = ClassDesc.ofDescriptor(STR."L\{className.replace(".", "/")};");
                byte[] classByteCode = classFile.build(thisClassDesc, classBuilder -> {
                    generatorCtor(classBuilder, thisClassDesc, targetClass);
                    ArrayList<Consumer<CodeBuilder>> clinitBlocks = new ArrayList<>();
                    Consumer<CodeBuilder> interfaceClinit = implementStructMarkInterface(classBuilder, thisClassDesc);
                    clinitBlocks.add(interfaceClinit);
                    for (Field field : targetClass.getDeclaredFields()) {
                        if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
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

                    classBuilder.withMethodBody("<clinit>", MethodTypeDesc.ofDescriptor("()V"), (AccessFlag.STATIC.mask()), it -> {
                        clinitBlocks.forEach(init -> init.accept(it));
                        it.return_();
                    });
                });

                if (classDataPeek != null) {
                    classDataPeek.accept(className, classByteCode);
                }

                aClass = lookup.defineClass(classByteCode);
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
            NativeGeneratorHelper.STRUCT_CONTEXT.remove();
        }
    }

    public <T> void register(Class<T> target) {
        enhance(target);
    }


    public void useLmf(boolean use_lmf) {
        this.use_lmf = use_lmf;
    }

    public VarHandle findFieldVarHandle(Field field) {
        Class<?> declaringClass = field.getDeclaringClass();
        MemoryLayout layout = layoutCaches.get(declaringClass);
        if (layout == null) {
            throw new StructException(STR."you should enhance \{declaringClass} first");
        }
        Class<?> type = field.getType();
        if (type.isPrimitive()
                || field.getAnnotation(Pointer.class) != null
                || Optional.ofNullable(field.getAnnotation(NativeArrayMark.class)).map(NativeArrayMark::asPointer).orElse(false)
        ) {
            return MethodHandles.insertCoordinates(layout.varHandle(MemoryLayout.PathElement.groupElement(field.getName())), 1, 0);
        }
        throw new StructException(STR."only support primitive type or pointer type");
    }

    static String generateProxyClassName(Class targetClass) {
        return STR."\{targetClass.getName()}_native_struct_proxy";
    }

    static String generateVarHandleName(Field field) {
        return STR."\{field.getName()}_native_struct_vh";
    }

    private Consumer<CodeBuilder> implementStructMarkInterface(ClassBuilder cb, ClassDesc thisClass) {

        cb.withField(GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class), AccessFlags.ofField(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL).flagsMask());
        cb.withField(LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class), AccessFlags.ofField(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL).flagsMask());
        cb.withField(MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class), AccessFlags.ofField(AccessFlag.PUBLIC).flagsMask());

        cb.withInterfaceSymbols(ClassFileHelper.toDesc(NativeStructEnhanceMark.class));
        cb.withMethodBody("fetchStructProxyGenerator", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.FETCH_STRUCT_PROXY_GENERATOR), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.getstatic(thisClass, GENERATOR_FIELD, ClassFileHelper.toDesc(StructProxyGenerator.class));
            it.areturn();
        });
        cb.withMethodBody("realMemory", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REAL_MEMORY), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.aload(0);
            it.getfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            it.areturn();
        });

        cb.withMethodBody("rebind", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REBIND_MEMORY), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.aload(1);
            it.aload(0);
            it.getfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            ClassFileHelper.invoke(it, NativeGeneratorHelper.REBIND_ASSERT_METHOD);
            it.aload(0);
            it.aload(1);
            it.putfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            it.return_();
        });
        cb.withMethodBody("layout", ClassFileHelper.toMethodDescriptor(NativeGeneratorHelper.REAL_MEMORY), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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

    private void generatorCtor(ClassBuilder cb, ClassDesc thisClass, Class originClass) {
        cb.withSuperclass(ClassFileHelper.toDesc(originClass));
        cb.withMethodBody("<init>", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.aload(0);
            it.invokespecial(ClassFileHelper.toDesc(originClass), "<init>", MethodTypeDesc.ofDescriptor("()V"));
            it.aload(0);
            it.aload(1);
            it.putfield(thisClass, MEMORY_FIELD, ClassFileHelper.toDesc(MemorySegment.class));
            it.return_();
        });
    }

    private Consumer<CodeBuilder> initVarHandleBlock(ClassBuilder cb, ClassDesc thisClass, Field field) {
        String varHandleFieldName = generateVarHandleName(field);
        cb.withField(varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class), AccessFlags.ofField(AccessFlag.FINAL, AccessFlag.STATIC, AccessFlag.PUBLIC).flagsMask());
        return (it) -> {
            it.getstatic(thisClass, LAYOUT_FIELD, ClassFileHelper.toDesc(MemoryLayout.class));
            it.ldc(field.getName());
            it.invokestatic(ClassFileHelper.toDesc(StructProxyGenerator.class), "generateVarHandle", ClassFileHelper.toMethodDescriptor(GENERATOR_VARHANDLE));
            it.putstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));
        };
    }

    private Consumer<CodeBuilder> generatePrimitiveFieldVarHandle(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout __) {

        String varHandleFieldName = STR."\{field.getName()}_native_struct_vh";
        cb.withMethodBody(STR."get\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."()\{ClassFileHelper.toSignature(field.getType())}"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.getstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));
            it.aload(0);
            ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);
            it.invokevirtual(ClassFileHelper.toDesc(VarHandle.class), "get", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)})\{ClassFileHelper.toSignature(field.getType())}"));
            it.returnInstruction(ClassFileHelper.calType(field.getType()));
        });

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(field.getType())})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.getstatic(thisClass, varHandleFieldName, ClassFileHelper.toDesc(VarHandle.class));
            it.aload(0);
            ClassFileHelper.invoke(it, REALMEMORY_METHOD, true);
            it.loadInstruction(ClassFileHelper.calType(field.getType()), 1);
            it.invokevirtual(ClassFileHelper.toDesc(VarHandle.class), "set", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)}\{ClassFileHelper.toSignature(field.getType())})V"));
            it.return_();
        });

        return initVarHandleBlock(cb, thisClass, field);
    }

    private Consumer<CodeBuilder> generateNativeArray(ClassBuilder cb, ClassDesc thisClass, Field field, MemoryLayout structLayout) {
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        if (arrayMark == null) {
            throw new IllegalArgumentException(STR."\{field} must be marked as NativeArrayMark");
        }
        long offset = structLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        long newSize = extract(arrayMark.size()).byteSize() * arrayMark.length();
        boolean asPointer = field.getAnnotation(Pointer.class) != null || arrayMark.asPointer();
        //不在生成出来的代码里面做分支 减少运行时分支判断
        cb.withMethodBody(STR."get\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."()\{ClassFileHelper.toSignature(NativeArray.class)}"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
            it.invokespecial(ClassFileHelper.toDesc(NativeArray.class), "<init>", MethodTypeDesc.ofDescriptor("(Ltop/dreamlike/panama/generator/proxy/StructProxyGenerator;Ljava/lang/foreign/MemorySegment;Ljava/lang/Class;)V"));
            it.areturn();
        });

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(NativeArray.class)})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            if (asPointer) {
                generateSetPtr(it, offset);
            } else {
                generateSetSubElement(it, offset, newSize);
            }
            it.return_();
        });

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
        cb.withMethodBody(STR."get\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."()\{ClassFileHelper.toSignature(MemorySegment.class)}"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
                ClassFileHelper.invoke(it, NativeGeneratorHelper.REINTERPRET, true);
                it.areturn();
            } else {
//                realMemory.asSlice(offset, realSize.byteSize() * arrayMark.length());
                it.ldc(offset);
                it.ldc(realSize.byteSize() * arrayMark.length());
                ClassFileHelper.invoke(it, NativeGeneratorHelper.AS_SLICE, true);
                it.areturn();
            }
        });

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
        cb.withMethodBody(STR."get\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."()\{ClassFileHelper.toSignature(field.getType())}"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
                ClassFileHelper.invoke(it, NativeGeneratorHelper.REINTERPRET, true);
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

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(field.getType())})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            if (isPointer) {
                generateSetPtr(it, offset);
            } else {
                generateSetSubElement(it, offset, subStructLayoutSize);
            }
            it.return_();
        });

        cb.withMethodBody(STR."set\{upperFirstChar(field.getName())}", MethodTypeDesc.ofDescriptor(STR."(\{ClassFileHelper.toSignature(MemorySegment.class)})V"), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
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
}

