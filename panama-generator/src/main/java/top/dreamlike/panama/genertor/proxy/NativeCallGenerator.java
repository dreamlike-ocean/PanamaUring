package top.dreamlike.panama.genertor.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.member.HandleInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.utility.JavaConstant;
import top.dreamlike.panama.genertor.annotation.NativeFunction;
import top.dreamlike.panama.genertor.annotation.Pointer;
import top.dreamlike.panama.genertor.exception.StructException;
import top.dreamlike.panama.genertor.helper.MethodVariableAccessLoader;
import top.dreamlike.panama.genertor.helper.NativeHelper;
import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.io.File;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * https://shipilev.net/jvm/anatomy-quarks/17-trust-nonstatic-final-fields/
 * 参考这个使用final进行优化
 */
public class NativeCallGenerator {
    private final ByteBuddy byteBuddy;

    private final NativeLibLookup nativeLibLookup;

    private static final MethodHandle TRANSFORM_OBJECT_TO_STRUCT_MH;

    //一点辅助的技巧。。。
    private static ThreadLocal<NativeCallGenerator> currentGenerator = new ThreadLocal<>();

    static {
        try {
            TRANSFORM_OBJECT_TO_STRUCT_MH = MethodHandles.lookup()
                    .findStatic(NativeCallGenerator.class, "transToPoint", MethodType.methodType(MemorySegment.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    final Map<Class<?>, Supplier<Object>> ctorCaches = new ConcurrentHashMap<>();
    private final StructProxyGenerator structProxyGenerator;

    public NativeCallGenerator() {
        this.structProxyGenerator = new StructProxyGenerator();
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLibLookup();
    }

    public NativeCallGenerator(StructProxyGenerator structProxyGenerator) {
        this.structProxyGenerator = structProxyGenerator;
        this.byteBuddy = new ByteBuddy(ClassFileVersion.JAVA_V21);
        this.nativeLibLookup = new NativeLibLookup();
    }

    private static MemorySegment transToPoint(Object o) {
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

    public static MethodHandle generateInGeneratorContext(Class interfaceClass, String methodName, MethodType methodType) throws NoSuchMethodException {
        NativeCallGenerator current = currentGenerator.get();
        if (current == null) {
            throw new IllegalArgumentException("must in context!");
        }
        //第一个是this指针 去掉
        methodType = methodType.dropParameterTypes(0, 1);
        Method method = interfaceClass.getDeclaredMethod(methodName, methodType.parameterArray());
        return current.nativeMethodHandle(method);
    }


    private static boolean needTransToPointer(Parameter parameter) {
        Class<?> typeClass = parameter.getType();
        return typeClass.isAssignableFrom(MemorySegment.class)
                || typeClass.isAssignableFrom(NativeStructEnhanceMark.class)
                || (!typeClass.isPrimitive() && parameter.getAnnotation(Pointer.class) != null);
    }

    private MethodHandle nativeMethodHandle(Method method) {
        NativeFunction function = method.getAnnotation(NativeFunction.class);
        ArrayList<Linker.Option> options = new ArrayList<>(2);
        if (function != null && function.fast()) {
            options.add(Linker.Option.isTrivial());
        }
        boolean allowPassHeap = function != null && function.allowPassHeap();
        String functionName = function == null ? method.getName() : function.value();

        ArrayList<Integer> pointIndex = new ArrayList<>();
        MemoryLayout[] layouts = new MemoryLayout[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Class<?> typeClass = parameters[i].getType();
            if (needTransToPointer(parameters[i])) {
                layouts[i] = ValueLayout.ADDRESS;
                pointIndex.add(i);
                continue;
            }

            layouts[i] = structProxyGenerator.extract(typeClass);
        }
        FunctionDescriptor fd = method.getReturnType() == void.class
                ? FunctionDescriptor.ofVoid(layouts)
                : FunctionDescriptor.of(structProxyGenerator.extract(method.getReturnType()), layouts);
        MethodHandle methodHandle = nativeLibLookup.downcallHandle(functionName, fd, options.toArray(Linker.Option[]::new));
        for (Integer i : pointIndex) {
            methodHandle = MethodHandles.filterArguments(
                    methodHandle,
                    i,
                    TRANSFORM_OBJECT_TO_STRUCT_MH
            );
        }
        return methodHandle;
    }

    @SuppressWarnings("unchecked")
    private Supplier<Object> bind(Class<?> nativeInterface) {
        try {
            if (!nativeInterface.isInterface()) {
                throw new IllegalArgumentException(STR. "\{ nativeInterface } is not interface" );
            }
            currentGenerator.set(this);
            String className = STR. "\{ nativeInterface.getName() }_native_call_enhance" ;
            var definition = byteBuddy.subclass(Object.class)
                    .implement(nativeInterface)
                    .name(className);
            Implementation.Composable cInitBlock = MethodCall.invoke(NativeHelper.EMPTY_METHOD);
            for (Method method : nativeInterface.getMethods()) {
                if (method.isBridge() || method.isDefault() || method.isSynthetic()) {
                    continue;
                }
                String mhFieldName = STR. "\{ method.getName() }_native_method_handle" ;
                //cInit 类初始化的时候赋值下静态字段
                cInitBlock = cInitBlock.andThen(MethodCall.invoke(NativeCallGenerator.class.getMethod("generateInGeneratorContext", Class.class, String.class, MethodType.class))
                        .with(nativeInterface, method.getName()).with(JavaConstant.MethodType.of(method)).setsField(named(mhFieldName)));
                definition = definition
                        .defineField(mhFieldName, MethodHandle.class, Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
                        .defineMethod(method.getName(), method.getReturnType(), Modifier.PUBLIC)
                        .withParameters(method.getParameterTypes())
                        //todo 如果返回一个结构体 or MemorySegment怎么办
                        .intercept(new Implementation.Simple(new ConstFieldInvokerStackManipulation(className, mhFieldName, method)));

            }

            definition = definition.invokable(MethodDescription::isTypeInitializer)
                    .intercept(cInitBlock);
            DynamicType.Unloaded<Object> unloaded = definition.make();
            if (structProxyGenerator.proxySavePath != null) {
                unloaded.saveIn(new File(structProxyGenerator.proxySavePath));
            }

            Class<?> aClass = unloaded.load(nativeInterface.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                    .getLoaded();
            //强制初始化执行cInit
            MethodHandles.lookup().ensureInitialized(aClass);
            return NativeHelper.ctorBinder(MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(void.class)), aClass);
        } catch (Throwable e) {
            throw new StructException("should not reach here!", e);
        } finally {
            currentGenerator.remove();
        }
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
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, className.replace(".", "/"), mhFieldName, "Ljava/lang/invoke/MethodHandle;");
            Size vistStaticSize = new Size(1, 1);
            res = res.aggregate(vistStaticSize);
            StackManipulation[] manipulations = new StackManipulation[method.getParameterCount() + 2];
            Parameter[] parameters = method.getParameters();
            int offset = 1;
            Class[] classes = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Class<?> type = parameters[i].getType();
                MethodVariableAccessLoader loader = MethodVariableAccessLoader.calLoader(type, offset);
                int targetAddOffset = loader.targetOffset();
                StackManipulation wait = loader.loadOp();
                offset += targetAddOffset;
                manipulations[i] = wait;
                if (NativeCallGenerator.needTransToPointer(parameters[i])) {
                    //擦除了防止跟methodHandle = MethodHandles.filterArguments(
                    //                    methodHandle,
                    //                    i,
                    //                    TRANSFORM_OBJECT_TO_STRUCT_MH
                    //            ); 的签名不一致
                    classes[i] = Object.class;
                } else {
                    classes[i] = type;
                }
            }

            JavaConstant.MethodType methodType = JavaConstant.MethodType.of(method.getReturnType(), classes);
            HandleInvocation handleInvocation = new HandleInvocation(methodType);
            manipulations[parameters.length] = handleInvocation;
            Class returnType = method.getReturnType();

            manipulations[parameters.length + 1] = calMethodReturn(returnType);

            StackManipulation stackManipulation = new StackManipulation.Compound(manipulations);
            res = res.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
            return res;
        }

    }
}
