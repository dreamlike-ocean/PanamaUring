package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.NativeFunctionPointer;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.ClassFileHelper;
import top.dreamlike.panama.generator.helper.DowncallContext;
import top.dreamlike.panama.generator.helper.FunctionPointer;
import top.dreamlike.panama.generator.helper.NativeAddressable;
import top.dreamlike.panama.generator.helper.NativeGeneratorHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
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

    static final String GENERATOR_FIELD_NAME = "_generator";
    private static final MethodHandle DLSYM_MH;
    private static final Method GENERATE_IN_GENERATOR_CONTEXT;

    static {
        try {
            DLSYM_MH = MethodHandles.lookup().findVirtual(NativeCallGenerator.class, "dlsym", MethodType.methodType(MemorySegment.class, String.class));
            GENERATE_IN_GENERATOR_CONTEXT = NativeCallGenerator.class.getMethod("generateInGeneratorContext", Class.class, String.class, MethodType.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, Supplier<Object>> ctorCaches = new ConcurrentHashMap<>();
    private final NativeLookup nativeLibLookup;
    private final ClassFile classFile = ClassFile.of();
    private final Map<String, MemorySegment> foreignFunctionAddressCache = new ConcurrentHashMap<>();
    private final StructProxyGenerator structProxyGenerator;
    public volatile boolean use_lmf = !NativeImageHelper.inExecutable();
    private volatile boolean use_indy = !NativeImageHelper.inExecutable();

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

        MethodHandle nativeCallMH = generator.nativeMethodHandle(method);
        return new ConstantCallSite(nativeCallMH);
    }

    private static boolean needTransToPointer(Parameter parameter) {
        Class<?> typeClass = parameter.getType();
        return MemorySegment.class.isAssignableFrom(typeClass)
               || NativeAddressable.class.isAssignableFrom(typeClass)
               || (!typeClass.isPrimitive() && parameter.getAnnotation(Pointer.class) != null);
    }

    public static void loadSo(Class<?> interfaceClass) throws IOException {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        CLib annotation = interfaceClass.getAnnotation(CLib.class);
        if (annotation == null || annotation.value().isBlank()) return;
        if (annotation.isLib()) {
            Runtime.getRuntime().loadLibrary(annotation.value());
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

    public void indyMode() {
        use_indy = true;
    }

    @SuppressWarnings("unchecked")
    public <T> T generate(Class<T> nativeInterface) {
        return generateSupplier(nativeInterface).get();
    }

    public <T> Supplier<T> generateSupplier(Class<T> nativeInterface) {
        Objects.requireNonNull(nativeInterface);
        try {
            return (Supplier<T>) ctorCaches.computeIfAbsent(nativeInterface, key -> bind(nativeInterface));
        } catch (Throwable throwable) {
            throw new StructException("should not reach here!", throwable);
        }
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

    /**
     * This private method handles the native method call based on the given Method.
     *
     * @param method The Method object representing the native method.
     * @return The MethodHandle object for the native method call.
     * @throws IllegalArgumentException if the return type of the method is not a primitive type or marked as returnIsPointer.
     */
    MethodHandle nativeMethodHandle(Method method) {
        DowncallContext downcallContext = parseDowncallContext(method);

        String functionName = downcallContext.functionName();
        FunctionDescriptor fd = downcallContext.fd();
        Linker.Option[] options = downcallContext.ops();
        boolean returnPointer = downcallContext.returnPointer();
        DowncallContext.CaptureContext captureContext = downcallContext.captureContext();
        boolean needCaptureStatue = captureContext.needCaptureStatue();
        boolean haveFp = downcallContext.containFp();
        ArrayList<Integer> rawMemoryIndex = downcallContext.rawMemoryIndex();

        MethodHandle methodHandle = Linker.nativeLinker().downcallHandle(fd, options);
        if (!haveFp) {
            methodHandle = methodHandle.bindTo(dlsym(functionName));
        }

        int allocatorIndex = -1;
        for (int i = 0; i < methodHandle.type().parameterCount(); i++) {
            if (methodHandle.type().parameterType(i) == SegmentAllocator.class) {
                allocatorIndex = i;
                break;
            }
        }
        boolean returnStruct = allocatorIndex != -1;
        if (returnStruct) {
            methodHandle = MethodHandles.collectArguments(
                    methodHandle,
                    allocatorIndex,
                    NativeLookup.CURRENT_ALLOCTOR_MH
            );
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

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();

            if (parameterType == String.class) {
                methodHandle = MethodHandles.filterArguments(
                        methodHandle,
                        i,
                        NativeLookup.JAVASTR_CSTR_MH
                );
                continue;
            }

            if (!parameterType.isArray()) {
                continue;
            }
            MethodHandle wrapperMH = NativeLookup.heapAccessMH(parameterType.getComponentType());
            methodHandle = MethodHandles.filterArguments(
                    methodHandle,
                    i,
                    wrapperMH
            );
        }

        if (needCaptureStatue) {
            methodHandle = captureContext.auto() ? NativeLookup.fillErrorAfterReturn(methodHandle) : NativeLookup.fillErrorAfterReturn(methodHandle, captureContext.errorNoTypes());
        }

        if (MemorySegment.class.isAssignableFrom(method.getReturnType())) {
            return methodHandle;
        }

        if (returnPointer || returnStruct) {
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
            //todo可以直接返回ctor的method
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

        if (method.getReturnType() == String.class) {
            methodHandle = MethodHandles.filterReturnValue(
                    methodHandle,
                    NativeLookup.CSTR_TOSTRING_MH
            );
        }

        return methodHandle;
    }

    DowncallContext parseDowncallContext(Method method) {
        NativeFunction function = method.getAnnotation(NativeFunction.class);
        String functionName = function == null || Objects.requireNonNullElse(function.value(), "").isBlank()
                ? method.getName()
                : function.value();

        CLib lib = method.getDeclaringClass().getAnnotation(CLib.class);
        if (lib != null) {
            functionName = lib.prefix() + functionName + lib.suffix();
        }

        Class<?> returnType = method.getReturnType();
        boolean returnPointer = !returnType.isPrimitive() && function != null && function.returnIsPointer();
        ArrayList<Linker.Option> options = new ArrayList<>(2);

        boolean needFast = function != null && function.fast();
        boolean needHeap = function != null && function.allowPassHeap();

        boolean needCaptureStatue = function != null && function.needErrorNo();
        ErrorNo.ErrorNoType[] types = null;
        boolean autoError = false;
        if (needCaptureStatue) {
            types = function.errorNoType();
            autoError = Arrays.stream(types).anyMatch(errorNoType -> errorNoType == ErrorNo.ErrorNoType.AUTO);
            options.add(NativeLookup.guessErrorNoOption(autoError, types));
        }

        ArrayList<Integer> rawMemoryIndex = new ArrayList<>();
        MemoryLayout[] layouts = new MemoryLayout[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        boolean hasFp = false;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.getAnnotation(NativeFunctionPointer.class) != null) {
                if (hasFp) {
                    throw new IllegalArgumentException("only one function pointer is allowed");
                }

                if (!MemorySegment.class.isAssignableFrom(parameter.getType())) {
                    throw new IllegalArgumentException("function pointer must be MemorySegment or its sub class");
                }

                if (i != 0) {
                    //虽然不限定位置也能做 比如利用permuteArguments做 但是这样代码写起来有点麻烦
                    //收益不大 所以还是固定第一个吧
                    throw new IllegalArgumentException("function pointer must be the first parameter");
                }
                hasFp = true;
                continue;
            }

            Class<?> typeClass = parameter.getType();

            //string转c str
            if (typeClass == String.class) {
                layouts[i] = ValueLayout.ADDRESS;
                continue;
            }

            if (needTransToPointer(parameter)) {
                layouts[i] = ValueLayout.ADDRESS;
                rawMemoryIndex.add(i);
                continue;
            }

            if (typeClass.isArray()) {
                if (!typeClass.getComponentType().isPrimitive()) {
                    throw new IllegalArgumentException("array must be primitive type");
                }
                needFast = needHeap = true;
                layouts[i] = ValueLayout.ADDRESS;
                continue;
            }

            layouts[i] = structProxyGenerator.extract(typeClass);
            if (!typeClass.isPrimitive()) {
                rawMemoryIndex.add(i);
            }
        }

        if (hasFp) {
            //去除第一个参数的布局。。。
            //java没有slice真sb啊
            MemoryLayout[] newLayout = new MemoryLayout[layouts.length - 1];
            System.arraycopy(layouts, 1, newLayout, 0, newLayout.length);
            layouts = newLayout;
        }

        MemoryLayout returnLayout;
        if (MemorySegment.class.isAssignableFrom(returnType) || returnPointer || returnType == String.class) {
            returnLayout = ValueLayout.ADDRESS;
        } else {
            returnLayout = structProxyGenerator.extract(returnType);
        }

        if (needFast || needHeap) {
            options.add(Linker.Option.critical(needHeap));
        }

        FunctionDescriptor fd = returnType == void.class
                ? FunctionDescriptor.ofVoid(layouts)
                : FunctionDescriptor.of(
                returnLayout,
                layouts
        );
        return new DowncallContext(fd, options.toArray(Linker.Option[]::new), functionName, returnPointer, new DowncallContext.CaptureContext(needCaptureStatue,autoError, types), rawMemoryIndex, hasFp);
    }

    private Class generateRuntimeProxyClass(MethodHandles.Lookup lookup, Class nativeInterface) throws IllegalAccessException {
        String className = generateProxyClassName(nativeInterface);
        var thisClassDesc = ClassDesc.ofDescriptor("L" + className.replace(".", "/") + ";");
        var thisClass = classFile.build(thisClassDesc, classBuilder -> {
            classBuilder.withInterfaceSymbols(ClassFileHelper.toDesc(nativeInterface));
            classBuilder.withField(GENERATOR_FIELD_NAME, ClassFileHelper.toDesc(NativeCallGenerator.class), AccessFlag.PUBLIC.mask() | AccessFlag.STATIC.mask() | AccessFlag.FINAL.mask());
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
            classBuilder.withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, Modifier.PUBLIC, it -> {
                        it.aload(0);
                        it.invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void);
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
            classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, (AccessFlag.STATIC.mask()), it -> {
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
        ClassDesc thisClassDesc = ClassDesc.of(className);
        thisClass.withMethodBody(method.getName(), ClassFileHelper.toMethodDescriptor(method), AccessFlag.PUBLIC.mask(), it -> {
            it.getstatic(thisClassDesc, mhFieldName, ClassFileHelper.toDesc(MethodHandle.class));
            ClassFileHelper.invokeMethodHandleExactWithAllArgs(method, it);
        });
        thisClass.withField(mhFieldName, ClassFileHelper.toDesc(MethodHandle.class), ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
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
        thisClass.withMethodBody(method.getName(), ClassFileHelper.toMethodDescriptor(method), ClassFile.ACC_PUBLIC, it -> {
            ClassFileHelper.loadAllArgs(method, it);
            it.invokedynamic(
                    DynamicCallSiteDesc.of(
                            ConstantDescs.ofCallsiteBootstrap(ClassFileHelper.toDesc(InvokeDynamicFactory.class), "nativeCallIndyFactory", ConstantDescs.CD_CallSite),
                            method.getName(),
                            ClassFileHelper.toMethodDescriptor(method)
                    )
            );
            ClassFileHelper.returnValue(it, method.getReturnType());
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
        String proxyName = nativeInterface.getName() + "_native_call_enhance";
        if (use_indy) {
            proxyName = proxyName + "_indy";
        }
        return proxyName;
    }

    private MemorySegment dlsym(String name) {
        Objects.requireNonNull(name);
        return foreignFunctionAddressCache.computeIfAbsent(name, nativeLibLookup::findOrException);
    }
}
