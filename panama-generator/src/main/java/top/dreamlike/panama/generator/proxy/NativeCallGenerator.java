package top.dreamlike.panama.generator.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.InvokeDynamic;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.HandleInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.JavaConstant;
import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.helper.MethodVariableAccessLoader;
import top.dreamlike.panama.generator.helper.NativeGeneratorHelper;
import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import java.io.*;
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <a href="https://shipilev.net/jvm/anatomy-quarks/17-trust-nonstatic-final-fields/">为什么实例中的final不可以信任</a>
 * 参考这个使用static final进行优化
 */
public class NativeCallGenerator {

    private final ByteBuddy byteBuddy;

    private static final Method INDY_BOOTSTRAP_METHOD;

    private final NativeLookup nativeLibLookup;

    private static final MethodHandle TRANSFORM_OBJECT_TO_STRUCT_MH;

    //一点辅助的技巧。。。
    private static final ThreadLocal<NativeCallGenerator> currentGenerator = new ThreadLocal<>();

    private static final String GENERATOR_FIELD_NAME = "_generator";

    private static final int CURRENT_JAVA_MAJOR_VERSION = Runtime.version().feature();

    static {
        try {
            TRANSFORM_OBJECT_TO_STRUCT_MH = MethodHandles.lookup().findStatic(NativeCallGenerator.class, "transToStruct", MethodType.methodType(MemorySegment.class, Object.class));
            NativeGeneratorHelper.fetchCurrentNativeCallGenerator = currentGenerator::get;
            INDY_BOOTSTRAP_METHOD = NativeCallGenerator.class.getMethod("indyFactory", MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private volatile boolean use_indy = false;

    private volatile boolean use_lmf = true;

    final Map<Class<?>, Supplier<Object>> ctorCaches = new ConcurrentHashMap<>();
    private final StructProxyGenerator structProxyGenerator;

    public NativeCallGenerator() {
        this.structProxyGenerator = new StructProxyGenerator();
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLookup();
    }

    public NativeCallGenerator(StructProxyGenerator structProxyGenerator) {
        this.structProxyGenerator = structProxyGenerator;
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
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

    public void indyMode() {
        use_indy = true;
    }

    private static MemorySegment transToStruct(Object o) {
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
                    .get();
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


    public void loadSo(Class<?> interfaceClass) throws IOException {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        CLib annotation = interfaceClass.getAnnotation(CLib.class);
        if (annotation == null || annotation.value().isBlank()) return;
        InputStream inputStream = annotation.inClassPath()
                ? classLoader.getResourceAsStream(STR."\{annotation.value()}")
                : new FileInputStream(annotation.value());
        if (inputStream == null) {
            throw new IllegalArgumentException(STR."cant find clib! classloader: \{classLoader}, path: \{annotation.value()}");
        }
        String tmpFileName = STR. "\{ interfaceClass.getSimpleName() }_\{ UUID.randomUUID().toString() }_dynamic.so" ;
        File file = File.createTempFile(tmpFileName, ".tmp");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        file.deleteOnExit();
        try (fileOutputStream; inputStream) {
            inputStream.transferTo(fileOutputStream);
            System.load(file.getAbsolutePath());
        }
    }

    private boolean needTransToPointer(Parameter parameter) {
        Class<?> typeClass = parameter.getType();
        return typeClass.isAssignableFrom(MemorySegment.class)
                || NativeStructEnhanceMark.class.isAssignableFrom(typeClass)
                || (!typeClass.isPrimitive() && parameter.getAnnotation(Pointer.class) != null);
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
    private MethodHandle nativeMethodHandle(Method method) {
        NativeFunction function = method.getAnnotation(NativeFunction.class);
        boolean returnPointer = function != null && function.returnIsPointer();
        ArrayList<Linker.Option> options = new ArrayList<>(2);
        if (function != null && function.fast()) {
            options.add(Linker.Option.isTrivial());
        }

        boolean needCaptureStatue = function != null && function.needErrorNo();
        if (needCaptureStatue) {
            options.add(Linker.Option.captureCallState("errno"));
        }

        if ((NativeLookup.primitiveMapToMemoryLayout(method.getReturnType()) == null && !returnPointer) && method.getReturnType() != void.class) {
            throw new IllegalArgumentException(STR. "\{ method } must return primitive type or is marked returnIsPointer" );
        }

        boolean allowPassHeap = function != null && function.allowPassHeap();
        String functionName = function == null || Objects.requireNonNullElse(function.value(), "").isBlank()
                ? method.getName()
                : function.value();

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
        if (MemorySegment.class.isAssignableFrom(method.getReturnType()) || returnPointer) {
            returnLayout = ValueLayout.ADDRESS;
        } else {
            returnLayout = structProxyGenerator.extract(method.getReturnType());
        }
        FunctionDescriptor fd = method.getReturnType() == void.class
                ? FunctionDescriptor.ofVoid(layouts)
                : FunctionDescriptor.of(
                returnLayout,
                layouts
        );
        MethodHandle methodHandle = nativeLibLookup.downcallHandle(functionName, fd, options.toArray(Linker.Option[]::new));

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

    /**
     * Binds a native interface to a dynamically generated implementation using byteBuddy library.
     *
     * @param nativeInterface The native interface to bind. Must be an interface.
     * @return A supplier that provides an instance of the dynamically generated implementation for the native interface.
     * @throws IllegalArgumentException If the nativeInterface is not an interface.
     * @throws StructException          If an error occurs during the binding process.
     */
    @SuppressWarnings("unchecked")
    private Supplier<Object> bind(Class<?> nativeInterface) {
        try {
            if (!nativeInterface.isInterface()) {
                throw new IllegalArgumentException(STR. "\{ nativeInterface } is not interface" );
            }
            String className = STR."\{nativeInterface.getName()}_native_call_enhance";
            currentGenerator.set(this);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(nativeInterface, MethodHandles.lookup());
            Class<?> aClass = null;
            try {
                aClass = lookup.findClass(className);
            } catch (ClassNotFoundException ignore) {
            }
            //证明没有生成过
            if (aClass == null) {
                var definition = byteBuddy.subclass(Object.class)
                        .implement(nativeInterface)
                        .defineField(GENERATOR_FIELD_NAME, NativeCallGenerator.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                        .name(className);
                Implementation.Composable cInitBlock = MethodCall.invoke(NativeGeneratorHelper.FETCH_CURRENT_NATIVE_CALL_GENERATOR).setsField(named(GENERATOR_FIELD_NAME))
                        .andThen(MethodCall.invoke(NativeGeneratorHelper.LOAD_SO).onField(GENERATOR_FIELD_NAME).with(nativeInterface));

                for (Method method : nativeInterface.getMethods()) {
                    if (method.isBridge() || method.isDefault() || method.isSynthetic()) {
                        continue;
                    }
                    if (!use_indy) {
                        String mhFieldName = STR."\{method.getName()}_native_method_handle";
                        //使用传统的static final的方案
                        //cInit 类初始化的时候赋值下静态字段
                        cInitBlock = cInitBlock
                                .andThen(MethodCall.invoke(NativeCallGenerator.class.getMethod("generateInGeneratorContext", Class.class, String.class, MethodType.class))
                                        .onField(GENERATOR_FIELD_NAME).with(nativeInterface, method.getName()).with(JavaConstant.MethodType.of(method)).setsField(named(mhFieldName)));
                        definition = definition
                                .defineField(mhFieldName, MethodHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                                .defineMethod(method.getName(), method.getReturnType(), Modifier.PUBLIC)
                                .withParameters(method.getParameterTypes())
                                .intercept(new Implementation.Simple(new ConstFieldInvokerStackManipulation(className, mhFieldName, method)));
                        continue;
                    }

                    InvokeDynamic nativeCallIndy = InvokeDynamic.bootstrap(INDY_BOOTSTRAP_METHOD).withMethodArguments();
                    definition = definition.defineMethod(method.getName(), method.getReturnType(), Modifier.PUBLIC)
                            .withParameters(method.getParameterTypes())
                            .intercept(nativeCallIndy);

                }

                definition = definition.invokable(MethodDescription::isTypeInitializer)
                        .intercept(cInitBlock);
                DynamicType.Unloaded<Object> unloaded = definition.make();
                if (structProxyGenerator.proxySavePath != null) {
                    unloaded.saveIn(new File(structProxyGenerator.proxySavePath));
                }
                aClass = unloaded.load(nativeInterface.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(lookup))
                        .getLoaded();
            }
            //强制初始化执行cInit
            lookup.ensureInitialized(aClass);
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
            currentGenerator.remove();
        }
    }

    public void useLmf(boolean use_lmf) {
        this.use_lmf = use_lmf;
    }

    static MethodReturn calMethodReturn(Class c) {
        if (c == int.class) return MethodReturn.INTEGER;
        if (c == double.class) return MethodReturn.DOUBLE;
        if (c == float.class) return MethodReturn.FLOAT;
        if (c == long.class) return MethodReturn.LONG;
        if (c == void.class) return MethodReturn.VOID;
        return MethodReturn.REFERENCE;
    }

    private record ConstFieldInvokerStackManipulation(String className, String mhFieldName,
                                                      Method method) implements StackManipulation {

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size res = Size.ZERO;
            //获取静态的对应mh字段
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className.replace(".", "/"), mhFieldName, "Ljava/lang/invoke/MethodHandle;");
            Size vistStaticSize = new Size(1, 1);
            res = res.aggregate(vistStaticSize);
            StackManipulation[] manipulations = new StackManipulation[method.getParameterCount() + 2];
            Parameter[] parameters = method.getParameters();
            int offset = 1;
            //压栈
            Class[] classes = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class<?> type = parameters[i].getType();
                MethodVariableAccessLoader loader = MethodVariableAccessLoader.calLoader(type, offset);
                int targetAddOffset = loader.targetOffset();
                StackManipulation wait = loader.loadOp();
                offset += targetAddOffset;
                manipulations[i] = wait;
                classes[i] = type;
            }

            JavaConstant.MethodType methodType = JavaConstant.MethodType.of(method.getReturnType(), classes);
            //调用handle的invokeExact
            HandleInvocation handleInvocation = new HandleInvocation(methodType);
            manipulations[parameters.length] = handleInvocation;
            Class returnType = method.getReturnType();
            //准备返回值
            manipulations[parameters.length + 1] = calMethodReturn(returnType);

            StackManipulation stackManipulation = new StackManipulation.Compound(manipulations);
            res = res.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
            return res;
        }

    }
}
