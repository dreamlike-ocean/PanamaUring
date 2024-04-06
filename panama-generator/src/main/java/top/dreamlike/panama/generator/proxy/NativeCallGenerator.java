package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.*;

import java.io.*;
import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.*;
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static top.dreamlike.panama.generator.helper.NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH;

/**
 * <a href="https://shipilev.net/jvm/anatomy-quarks/17-trust-nonstatic-final-fields/">为什么实例中的final不可以信任</a>
 * 参考这个使用static final进行优化
 */
public class NativeCallGenerator {

    private static final MethodHandle DLSYM_MH;

    private static final Method INDY_BOOTSTRAP_METHOD;

    private final NativeLookup nativeLibLookup;

    private final ClassFile classFile = ClassFile.of();

    static {
        try {
            DLSYM_MH = MethodHandles.lookup().findVirtual(NativeCallGenerator.class, "dlsym", MethodType.methodType(MemorySegment.class, String.class));
            INDY_BOOTSTRAP_METHOD = InvokeDynamicFactory.class.getMethod("nativeCallIndyFactory", MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class);
            GENERATE_IN_GENERATOR_CONTEXT = NativeCallGenerator.class.getMethod("generateInGeneratorContext", Class.class, String.class, MethodType.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    static final String GENERATOR_FIELD_NAME = "_generator";

    private static final Method GENERATE_IN_GENERATOR_CONTEXT;

    public volatile boolean use_lmf = !NativeImageHelper.inExecutable();
    private Map<String, MemorySegment> foreignFunctionAddressCache = new ConcurrentHashMap<>();
    private volatile boolean use_indy = !NativeImageHelper.inExecutable();

    final Map<Class<?>, Supplier<Object>> ctorCaches = new ConcurrentHashMap<>();
    private final StructProxyGenerator structProxyGenerator;

    public NativeCallGenerator() {
        this.structProxyGenerator = new StructProxyGenerator();
        this.nativeLibLookup = new NativeLookup();
    }

    public NativeCallGenerator(StructProxyGenerator structProxyGenerator) {
        this.structProxyGenerator = structProxyGenerator;
        this.nativeLibLookup = new NativeLookup();
    }

    public static CallSite indyFactory(MethodHandles.Lookup lookup, String methodName, MethodType methodType, Object... args) throws Throwable {
        //这里的lookup是当前的代理类
        Class<?> lookupClass = lookup.lookupClass();
        NativeCallGenerator generator = (NativeCallGenerator) lookupClass.getField(GENERATOR_FIELD_NAME).get(null);
        Class<?> targetInterface = lookupClass.getInterfaces()[0];
        Method method = targetInterface.getMethod(methodName, methodType.parameterArray());
        //本来就是lazy的所以这里直接寻找对应符号地址然后绑定就行了
        MethodHandle nativeCallMH = generator.nativeMethodHandle(method, false);
        return new ConstantCallSite(nativeCallMH);
    }

    public void indyMode() {
        use_indy = true;
    }

    @SuppressWarnings("unchecked")
    public <T> T generate(Class<T> nativeInterface) {
        Objects.requireNonNull(nativeInterface);
        try {
            return (T) ctorCaches.computeIfAbsent(nativeInterface, key -> bind(nativeInterface))
                    .get();
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
    }

    private static boolean needTransToPointer(Parameter parameter) {
        Class<?> typeClass = parameter.getType();
        return MemorySegment.class.isAssignableFrom(typeClass)
                || NativeAddressable.class.isAssignableFrom(typeClass)
                || (!typeClass.isPrimitive() && parameter.getAnnotation(Pointer.class) != null);
    }

    public MethodHandle generateInGeneratorContext(Class interfaceClass, String methodName, MethodType methodType) throws NoSuchMethodException {
        NativeCallGenerator current = this;
        //第一个是this指针 去掉
        methodType = methodType.dropParameterTypes(0, 1);
        Method method = interfaceClass.getDeclaredMethod(methodName, methodType.parameterArray());
        return current.nativeMethodHandle(method);
    }

    public MemorySegment generateUpcall(Arena scope, Method method, Object... receiver) {
        //实例方法
        if (!Modifier.isStatic(method.getModifiers())) {
            if (receiver.length != 1) {
                throw new IllegalArgumentException("receiver length must be 1");
            }
            //判断当前method是否归于属receiver
            if (!method.getDeclaringClass().isAssignableFrom(receiver[0].getClass())) {
                throw new IllegalArgumentException("receiver type must be assignable from method declaring class");
            }
        }
        try {
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            if (!Modifier.isStatic(method.getModifiers())) {
                handle = handle.bindTo(receiver[0]);
            }
            Parameter[] parameters = method.getParameters();
            MemoryLayout[] memoryLayouts = new MemoryLayout[parameters.length];
            ArrayList<Integer> pointerIndex = new ArrayList<>(parameters.length / 2);
            for (int i = 0; i < parameters.length; i++) {
                Class<?> type = parameters[i].getType();
                if (type.isPrimitive()) {
                    memoryLayouts[i] = NativeLookup.primitiveMapToMemoryLayout(type);
                } else {
                    //默认是指针吧。。。
                    memoryLayouts[i] = ValueLayout.ADDRESS;
                    pointerIndex.add(i);
                }
            }
            MemoryLayout returnMemoryLayout = method.getReturnType().isPrimitive() ? NativeLookup.primitiveMapToMemoryLayout(method.getReturnType()) : ValueLayout.ADDRESS;

            FunctionDescriptor fd = method.getReturnType() == void.class
                    ? FunctionDescriptor.ofVoid(memoryLayouts)
                    : FunctionDescriptor.of(
                    returnMemoryLayout,
                    memoryLayouts
            );

            for (Integer index : pointerIndex) {
                int i = index;
                Class<?> argType = parameters[i].getType();
                MethodHandle argEnhanceMH = StructProxyGenerator.ENHANCE_MH
                        .asType(StructProxyGenerator.ENHANCE_MH.type().changeReturnType(argType))
                        .bindTo(structProxyGenerator)
                        .bindTo(argType);
                //第二步将MemorySegment转为对应的java对象
                handle = MethodHandles.filterArguments(
                        handle,
                        i,
                        argEnhanceMH
                );
                MemoryLayout layout = structProxyGenerator.extract(argType);
                //第一步 先从零长度的指针转换为对应长度的MemorySegment
                handle = MethodHandles.filterArguments(
                        handle,
                        i,
                        MethodHandles.insertArguments(
                                NativeGeneratorHelper.DEREFERENCE,
                                1,
                                layout.byteSize()
                        )
                );
            }
            //非原始类型则需要通过返回指针
            if (!method.getReturnType().isPrimitive()) {
                handle = MethodHandles.filterReturnValue(
                        handle,
                        TRANSFORM_OBJECT_TO_STRUCT_MH.asType(TRANSFORM_OBJECT_TO_STRUCT_MH.type().changeParameterType(0, method.getReturnType()))
                );
            }
            return Linker.nativeLinker()
                    .upcallStub(handle, fd, scope);
        } catch (Throwable t) {
            throw new StructException("should not reach here!", t);
        }
    }

    public FunctionPointer generateUpcallFP(Arena scope, Method method, Object... receiver) {
        return new FunctionPointer(generateUpcall(scope, method, receiver));
    }

    public void plainMode() {
        use_indy = false;
    }


    private MethodHandle nativeMethodHandle(Method method) {
        return nativeMethodHandle(method, false);
    }
    /**
     * This private method handles the native method call based on the given Method.
     *
     * @param method The Method object representing the native method.
     * @return The MethodHandle object for the native method call.
     * @throws IllegalArgumentException if the return type of the method is not a primitive type or marked as returnIsPointer.
     */
    MethodHandle nativeMethodHandle(Method method,boolean lazy) {
        DowncallContext downcallContext = parseDowncallContext(method);

        String functionName = downcallContext.functionName();
        FunctionDescriptor fd = downcallContext.fd();
        Linker.Option[] options = downcallContext.ops();
        boolean returnPointer = downcallContext.returnPointer();
        boolean needCaptureStatue = downcallContext.needCaptureStatue();
        ArrayList<Integer> rawMemoryIndex = downcallContext.rawMemoryIndex();
        if (needCaptureStatue && downcallContext.fast()) {
            throw new IllegalArgumentException("fast mode cant capture errno");
        }

//        MethodHandle methodHandle = nativeLibLookup.downcallHandle(functionName, fd, options);
        MethodHandle methodHandle = Linker.nativeLinker().downcallHandle(fd, options);
        if (lazy) {
            //延迟到第一次调用时去找符号
            MethodHandle dlsymMH = DLSYM_MH
                    .bindTo(this)
                    .bindTo(functionName);
            methodHandle = MethodHandles.collectArguments(
                    methodHandle,
                    0,
                    dlsymMH
            );
        } else {
            methodHandle = methodHandle.bindTo(dlsym(functionName));
        }

        if (needCaptureStatue) {
            /*
             * 因为是V filter2()这种模式的
             * 所以等价于摘掉第一个参数，用一个()->V类型的MethodHandle作为第一个参数的产出方
             * 等价于
             *  {
             *     MemorySegment errorBuffer = allocateErrorBuffer();
             *     nativeMethodHandle.invokeExact(
             *       errorBuffer ,
             *       ....
             *     )
             *  }
             */
            methodHandle = MethodHandles.collectArguments(
                    methodHandle,
                    0,
                    NativeLookup.AllocateErrorBuffer_MH
            );
        }
        var parameters = method.getParameters();

        for (Integer i : rawMemoryIndex) {
            //调用native的mh之前先把用户的java对象转化为MemorySegment
            methodHandle = MethodHandles.filterArguments(
                    methodHandle,
                    i,
                    TRANSFORM_OBJECT_TO_STRUCT_MH.asType(TRANSFORM_OBJECT_TO_STRUCT_MH.type().changeParameterType(0, parameters[i].getType()))
            );
        }

        if (needCaptureStatue) {
            /*
             *  别看fillErrorNoAfterReturn 太丑了。。SB java.。
             *  等价于
             *  {
             *    var returnValue = nativeMethodHandle.invokeExact(...);
             *    fillErrorNo();
             *    return returnValue;
             *  }
             */
            methodHandle = NativeLookup.fillErrorNoAfterReturn(methodHandle);
        }

        if (MemorySegment.class.isAssignableFrom(method.getReturnType())) {
            return methodHandle;
        }

        if (returnPointer) {
            MemoryLayout returnLayout = fd.returnLayout().get();
            //先调整返回值类型对应的memorySegment长度
            methodHandle = MethodHandles.filterReturnValue(
                    methodHandle,
                    MethodHandles.insertArguments(
                            NativeGeneratorHelper.DEREFERENCE,
                            1,
                            returnLayout.byteSize()
                    )
            );

            //绑定当前的StructProxyGenerator
            //绑定的结果是 (MemorySegment) -> ${returnType}
            MethodHandle returnEnhance = StructProxyGenerator.ENHANCE_MH
                    .asType(StructProxyGenerator.ENHANCE_MH.type().changeReturnType(method.getReturnType()))
                    .bindTo(structProxyGenerator)
                    .bindTo(method.getReturnType());
            //来把MemorySegment返回值转换为java bean
            methodHandle = MethodHandles.filterReturnValue(
                    methodHandle,
                    returnEnhance
            );
        }
        return methodHandle;
    }

    DowncallContext parseDowncallContext(Method method) {
        NativeFunction function = method.getAnnotation(NativeFunction.class);
        String functionName = function == null || Objects.requireNonNullElse(function.value(), "").isBlank()
                ? method.getName()
                : function.value();
        Class<?> returnType = method.getReturnType();
        boolean returnPointer = !returnType.isPrimitive() && function != null && function.returnIsPointer();
        ArrayList<Linker.Option> options = new ArrayList<>(2);
        boolean allowPassHeap = function != null && function.allowPassHeap();
        if (function != null && function.fast()) {
            options.add(Linker.Option.critical(allowPassHeap));
        }

        boolean needCaptureStatue = function != null && function.needErrorNo();
        if (needCaptureStatue) {
            options.add(Linker.Option.captureCallState("errno"));
        }

        if ((NativeLookup.primitiveMapToMemoryLayout(returnType) == null && !returnPointer && !MemorySegment.class.isAssignableFrom(returnType)) && returnType != void.class) {
            throw new IllegalArgumentException(method + " must return primitive type or is marked returnIsPointer");
        }
        ArrayList<Integer> rawMemoryIndex = new ArrayList<>();
        MemoryLayout[] layouts = new MemoryLayout[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Class<?> typeClass = parameters[i].getType();
            if (needTransToPointer(parameters[i])) {
                layouts[i] = ValueLayout.ADDRESS;
                rawMemoryIndex.add(i);
                continue;
            }
            layouts[i] = structProxyGenerator.extract(typeClass);
            if (!typeClass.isPrimitive()) {
                rawMemoryIndex.add(i);
            }
        }

        MemoryLayout returnLayout;
        if (MemorySegment.class.isAssignableFrom(returnType) || returnPointer) {
            returnLayout = ValueLayout.ADDRESS;
        } else {
            returnLayout = structProxyGenerator.extract(returnType);
        }
        FunctionDescriptor fd = returnType == void.class
                ? FunctionDescriptor.ofVoid(layouts)
                : FunctionDescriptor.of(
                returnLayout,
                layouts
        );
        return new DowncallContext(fd, options.toArray(Linker.Option[]::new), functionName, returnPointer, needCaptureStatue, rawMemoryIndex);
    }


    private Class generateRuntimeProxyClass(MethodHandles.Lookup lookup, Class nativeInterface) throws IllegalAccessException {
        String className = generateProxyClassName(nativeInterface);
        var thisClassDesc = ClassDesc.ofDescriptor("L" + className.replace(".", "/") + ";");
        var thisClass = classFile.build(thisClassDesc, classBuilder -> {
            classBuilder.withInterfaceSymbols(ClassFileHelper.toDesc(nativeInterface));
            classBuilder.withField(GENERATOR_FIELD_NAME, ClassFileHelper.toDesc(NativeCallGenerator.class), AccessFlags.ofField(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL).flagsMask());
            ArrayList<Consumer<CodeBuilder>> clinits = new ArrayList<>();
            //初始化内部的genertor字段
            clinits.add(it -> {
                ClassFileHelper.invoke(it, NativeGeneratorHelper.FETCH_CURRENT_NATIVE_CALL_GENERATOR);
                it.putstatic(thisClassDesc, GENERATOR_FIELD_NAME, ClassFileHelper.toDesc(NativeCallGenerator.class));
            });
            //加载动态库
            clinits.add(it -> {
                it.ldc(ClassFileHelper.toDesc(nativeInterface));
                ClassFileHelper.invoke(it, NativeGeneratorHelper.LOAD_SO);
            });
            classBuilder.withMethodBody("<init>", MethodTypeDesc.ofDescriptor("()V"), Modifier.PUBLIC, it -> {
                        it.aload(0);
                        it.invokespecial(ClassFileHelper.toDesc(Object.class),"<init>", MethodTypeDesc.ofDescriptor("()V"));
                        it.return_();
                    }
            );

            for (Method method : nativeInterface.getMethods()) {
                if (method.isBridge() || method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                if (!use_indy) {
                    Consumer<CodeBuilder> needInitInClint = invokeByMh(method, classBuilder, className);
                    clinits.add(needInitInClint);
                    continue;
                }
                invokeByIndy(method, classBuilder, className);
            }
            classBuilder.withMethodBody("<clinit>", MethodTypeDesc.ofDescriptor("()V"), (AccessFlag.STATIC.mask()), it -> {
                clinits.forEach(init -> init.accept(it));
                it.return_();
            });
        });
        if (structProxyGenerator.classDataPeek != null) {
            structProxyGenerator.classDataPeek.accept(className, thisClass);
        }
        return lookup.defineClass(thisClass);
    }

    private Consumer<CodeBuilder> invokeByMh(Method method, ClassBuilder thisClass, String className) {
        String mhFieldName = method.getName() + "_native_method_handle";
        ClassDesc thisClassDesc = ClassDesc.ofDescriptor("L" + className.replace(".", "/") + ";" );
        thisClass.withMethodBody(method.getName(), ClassFileHelper.toMethodDescriptor(method), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            it.getstatic(thisClassDesc, mhFieldName, ClassFileHelper.toDesc(MethodHandle.class));
            ClassFileHelper.invokeMethodHandleExactWithAllArgs(method, it);
        });
        thisClass.withField(mhFieldName, ClassFileHelper.toDesc(MethodHandle.class), AccessFlags.ofField(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL).flagsMask());
        return it -> {
            it.getstatic(thisClassDesc, GENERATOR_FIELD_NAME, ClassFileHelper.toDesc(NativeCallGenerator.class));
            ClassDesc nativeInterfaceClassDesc = ClassFileHelper.toDesc(method.getDeclaringClass());
            it.ldc(nativeInterfaceClassDesc);
            it.ldc(method.getName());
            it.ldc(ClassFileHelper.toMethodDescriptor(method).insertParameterTypes(0, nativeInterfaceClassDesc));
            ClassFileHelper.invoke(it, GENERATE_IN_GENERATOR_CONTEXT);
            it.putstatic(thisClassDesc, mhFieldName, ClassFileHelper.toDesc(MethodHandle.class));
        };
    }

    private void invokeByIndy(Method method, ClassBuilder thisClass, String className) {
        thisClass.withMethodBody(method.getName(), ClassFileHelper.toMethodDescriptor(method), AccessFlags.ofMethod(AccessFlag.PUBLIC).flagsMask(), it -> {
            ClassFileHelper.loadAllArgs(method, it);
            it.invokeDynamicInstruction(
                    DynamicCallSiteDesc.of(
                            MethodHandleDesc.ofMethod(
                                    DirectMethodHandleDesc.Kind.STATIC, ClassFileHelper.toDesc(InvokeDynamicFactory.class), "nativeCallIndyFactory",
                                    ClassFileHelper.toMethodDescriptor(INDY_BOOTSTRAP_METHOD)
                            ),
                            method.getName(),
                            ClassFileHelper.toMethodDescriptor(method)
                    )
            );
            it.returnInstruction(ClassFileHelper.calType(method.getReturnType()));
        });
    }

    /**
     * Binds a native interface to a dynamically generated implementation using byteBuddy library.
     *
     * @param nativeInterface The native interface to bind. Must be an interface.
     * @return A supplier that provides an instance of the dynamically generated implementation for the native interface.
     * @throws IllegalArgumentException If the nativeInterface is not an interface.
     * @throws StructException          If an error occurs during the binding process.
     */
    @SuppressWarnings("unchecked")
    Supplier<Object> bind(Class<?> nativeInterface) {
        try {
            if (!nativeInterface.isInterface()) {
                throw new IllegalArgumentException(nativeInterface + " is not interface");
            }
            String className = generateProxyClassName(nativeInterface);
            NativeGeneratorHelper.CURRENT_GENERATOR.set(this);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(nativeInterface, MethodHandles.lookup());
            Class<?> aClass = null;
            try {
                aClass = lookup.findClass(className);
            } catch (ClassNotFoundException ignore) {
            }
            //证明没有生成过
            if (aClass == null) {
                aClass = generateRuntimeProxyClass(lookup, nativeInterface);
            }
            //强制初始化执行cInit
            if (!structProxyGenerator.skipInit) {
                lookup.ensureInitialized(aClass);
            }
            MethodHandle methodHandle = MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(void.class));
            if (use_lmf) {
                return NativeGeneratorHelper.ctorBinder(methodHandle);
            }
            return () -> {
                var mh = methodHandle.asType(methodHandle.type().changeReturnType(Object.class));
                try {
                    return (Object) mh.invokeExact();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            };
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            NativeGeneratorHelper.CURRENT_GENERATOR.remove();
        }
    }

    public void useLmf(boolean use_lmf) {
        this.use_lmf = use_lmf;
    }

    String generateProxyClassName(Class nativeInterface) {
        return nativeInterface.getName() + "_native_call_enhance";
    }

    private MemorySegment dlsym(String name) {
        Objects.requireNonNull(name);
        return foreignFunctionAddressCache.computeIfAbsent(name, nativeLibLookup::findOrException);
    }

    public static void loadSo(Class<?> interfaceClass) throws IOException {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        CLib annotation = interfaceClass.getAnnotation(CLib.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (annotation.isLib()) {
            Runtime.getRuntime().loadLibrary(classLoader.getName());
            return;
        }

        InputStream inputStream = annotation.inClassPath()
                ? classLoader.getResourceAsStream(annotation.value())
                : new FileInputStream(annotation.value());
        if (inputStream == null) {
            throw new IllegalArgumentException("cant find clib! classloader: " + classLoader + ", path: " + annotation.value());
        }
        String tmpFileName = interfaceClass.getSimpleName() + "_" + UUID.randomUUID() + "_dynamic.so";
        File file = File.createTempFile(tmpFileName, ".tmp");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        file.deleteOnExit();
        try (fileOutputStream; inputStream) {
            inputStream.transferTo(fileOutputStream);
            System.load(file.getAbsolutePath());
        }
    }
}
