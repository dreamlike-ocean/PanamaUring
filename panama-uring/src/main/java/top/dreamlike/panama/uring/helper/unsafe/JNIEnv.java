package top.dreamlike.panama.uring.helper.unsafe;


import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static top.dreamlike.panama.uring.helper.unsafe.PanamaUnsafeHelper.throwable;

class JNIEnv {

    public final static int JNI_VERSION = 0x00150000;
    private final static MemorySegment MAIN_VM_Pointer = throwable(JNIEnv::initMainVM);
    private final static MethodHandle GET_JNIENV_MH = throwable(JNIEnv::initGetJNIEnvMH);

    private final static ThreadLocal<Object> jniToJava = new ThreadLocal<>();

    private final static MethodHandle NewStringPlatform = throwable(() -> {
        MemorySegment JNU_NewStringPlatformFP = SymbolLookup.loaderLookup()
                .find("JNU_NewStringPlatform")
                .get();
        return Linker.nativeLinker()
                .downcallHandle(FunctionDescriptor.of(
                        /*jstring*/ValueLayout.ADDRESS,
                        /*JNIEnv *env */ValueLayout.ADDRESS,
                        /*const char *str*/ ValueLayout.ADDRESS
                )).bindTo(JNU_NewStringPlatformFP);
    });
    public final JNIEnvFunctions functions;
    private final SegmentAllocator allocator;
    private final MemorySegment jniEnvPointer;


    public JNIEnv(SegmentAllocator allocator) {
        this.allocator = allocator;
        jniEnvPointer = initJniEnv();
        functions = new JNIEnvFunctions(jniEnvPointer);
    }

    private static Object getSecret() {
        return jniToJava.get();
    }

    private static void setSecret(Object o) {
        jniToJava.set(o);
    }

    private static MemorySegment initMainVM() throws Throwable {
        Runtime.getRuntime().loadLibrary("java");
        String javaHomePath = System.getProperty("java.home", "");
        if (javaHomePath.isBlank()) {
            throw new RuntimeException("cant find java.home!");
        }
        //根据当前系统判断使用哪个后缀名
        String libName = System.mapLibraryName("jvm");

        String jvmPath = javaHomePath + "/lib/server/" + libName;
        if (!Files.exists(Path.of(javaHomePath + "/lib/server/" + libName))) {
            jvmPath = javaHomePath + "/bin/server/" + libName;
        }
        Runtime.getRuntime().load(jvmPath);
        MemorySegment jniGetCreatedJavaVM_FP = SymbolLookup.loaderLookup()
                .find("JNI_GetCreatedJavaVMs")
                .get();
        MethodHandle JNI_GetCreatedJavaVM_MH = Linker.nativeLinker()
                .downcallHandle(
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
                )
                .bindTo(jniGetCreatedJavaVM_FP);
        Arena global = Arena.global();
        MemorySegment vm = global.allocate(ValueLayout.ADDRESS);
        MemorySegment numVMs = global.allocate(ValueLayout.JAVA_INT);
        //jdk22和其他版本 兼容使用
        numVMs.set(ValueLayout.JAVA_INT, 0, 0);
        int i = (int) JNI_GetCreatedJavaVM_MH.invokeExact(vm, 1, numVMs);
        return vm.get(ValueLayout.ADDRESS, 0);
    }

    private static MethodHandle initGetJNIEnvMH() {
        MemorySegment JNU_GetEnv_FP = SymbolLookup.loaderLookup()
                .find("JNU_GetEnv")
                .get();
        return Linker.nativeLinker()
                .downcallHandle(FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT))
                .bindTo(JNU_GetEnv_FP);
    }

    public MemorySegment NewGlobalRef(MemorySegment jobject) {
        try {
            return (MemorySegment) JNIEnvFunctions.NewGlobalRef_MH.invokeExact(functions.NewGlobalRefFp, jniEnvPointer, jobject);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void DeleteGlobalRef(MemorySegment globalRef) {
        try {
            JNIEnvFunctions.DeleteGlobalRef_MH.invokeExact(functions.DeleteGlobalRefFp, jniEnvPointer, globalRef);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public GlobalRef FindClass(Class c) {
        boolean isSystemClassloader = c.getClassLoader() == null;
        if (isSystemClassloader) {
            return throwable(() -> new GlobalRef(this, (MemorySegment) JNIEnvFunctions.FindClassMH.invokeExact(
                    functions.FindClassFp,
                    jniEnvPointer,
                    allocator.allocateFrom(c.getName().replace(".", "/"))))
            );
        }

        return throwable(() ->
        {
            try (GlobalRef threadRef = CallStaticMethodByName(Thread.class.getMethod("currentThread"));
                 GlobalRef classLoaderJobjectRef = CallMethodByName(Thread.class.getMethod("getContextClassLoader"), threadRef.ref());
                 GlobalRef classNameRef = cstrToJstring((allocator.allocateFrom(c.getName())))
            ) {
//                Class<?> name = Class.forName("top.dreamlike.unsafe.jni.JNIEnv", true, loader);
                MemorySegment segment = allocator.allocate(JValue.jvalueLayout);
                segment.set(ValueLayout.JAVA_BOOLEAN, 0, false);
                return CallStaticMethodByName(Class.class.getDeclaredMethod("forName", String.class, boolean.class, ClassLoader.class),
                        new JValue(classNameRef.ref().address()),
                        new JValue(segment.get(ValueLayout.JAVA_LONG, 0)),
                        new JValue(classLoaderJobjectRef.ref().address())
                );
            }
        });
    }

    public GlobalRef GetStaticFieldByName(Field field) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("only support static field");
        }
        return throwable(() -> {

            try (var clsRef = FindClass(field.getDeclaringClass())) {
                var fidRef = (MemorySegment) JNIEnvFunctions.GetStaticFieldID_MH.invokeExact(
                        functions.GetStaticFieldIDFp,
                        jniEnvPointer,
                        clsRef.ref(),
                        allocator.allocateFrom(field.getName()),
                        allocator.allocateFrom(PanamaUnsafeHelper.classToSig(field.getType()))
                );
                boolean isRef = false;
                long value = switch (field.getType().getName()) {
                    case "boolean" ->
                            (long) JNIEnvFunctions.GetStaticBooleanField_MH.invokeExact(functions.GetStaticBooleanFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "byte" ->
                            (long) JNIEnvFunctions.GetStaticByteField_MH.invokeExact(functions.GetStaticByteFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "char" ->
                            (long) JNIEnvFunctions.GetStaticCharField_MH.invokeExact(functions.GetStaticCharFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "short" ->
                            (long) JNIEnvFunctions.GetStaticShortField_MH.invokeExact(functions.GetStaticShortFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "int" ->
                            (long) JNIEnvFunctions.GetStaticIntField_MH.invokeExact(functions.GetStaticIntFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "long" ->
                            (long) JNIEnvFunctions.GetStaticLongField_MH.invokeExact(functions.GetStaticLongFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "float" ->
                            (long) JNIEnvFunctions.GetStaticFloatField_MH.invokeExact(functions.GetStaticFloatFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    case "double" ->
                            (long) JNIEnvFunctions.GetStaticDoubleField_MH.invokeExact(functions.GetStaticDoubleFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    default -> {
                        isRef = true;
                        yield (long) JNIEnvFunctions.GetStaticObjectField_MH.invokeExact(functions.GetStaticObjectFieldFp, jniEnvPointer, clsRef.ref(), fidRef);
                    }

                };
                return isRef ? new GlobalRef(this, MemorySegment.ofAddress(value)) : new GlobalRef(this, new JValue(value));
            }
        });
    }

    public void SetStaticFieldByName(Field field, GlobalRef value) {
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("only support static field");
        }
        throwable(() -> {
            Class<?> aClass = field.getDeclaringClass();

            try (GlobalRef clsRef = FindClass(aClass);) {
                var fidRef = (MemorySegment) JNIEnvFunctions.GetStaticFieldID_MH.invokeExact(
                        functions.GetStaticFieldIDFp,
                        jniEnvPointer,
                        clsRef.ref(),
                        allocator.allocateFrom(field.getName()),
                        allocator.allocateFrom(PanamaUnsafeHelper.classToSig(field.getType()))
                );
                switch (field.getType().getName()) {
                    case "boolean" ->
                            JNIEnvFunctions.SetStaticBooleanField_MH.invokeExact(functions.SetStaticBooleanFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getBoolean());
                    case "byte" ->
                            JNIEnvFunctions.SetStaticByteField_MH.invokeExact(functions.SetStaticByteFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getByte());
                    case "char" ->
                            JNIEnvFunctions.SetStaticCharField_MH.invokeExact(functions.SetStaticCharFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getChar());
                    case "short" ->
                            JNIEnvFunctions.SetStaticShortField_MH.invokeExact(functions.SetStaticShortFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getShort());
                    case "int" ->
                            JNIEnvFunctions.SetStaticIntField_MH.invokeExact(functions.SetStaticIntFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getInt());
                    case "long" ->
                            JNIEnvFunctions.SetStaticLongField_MH.invokeExact(functions.SetStaticLongFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getLong());
                    case "float" ->
                            JNIEnvFunctions.SetStaticFloatField_MH.invokeExact(functions.SetStaticFloatFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getFloat());
                    case "double" ->
                            JNIEnvFunctions.SetStaticDoubleField_MH.invokeExact(functions.SetStaticDoubleFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.jValue.getDouble());
                    default ->
                            JNIEnvFunctions.SetStaticObjectField_MH.invokeExact(functions.SetStaticObjectFieldFp, jniEnvPointer, clsRef.ref(), fidRef, value.ref());
                }
            }

        });
    }

    public GlobalRef GetFieldByName(Field field, GlobalRef jobject) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("only support not static field");
        }
        return throwable(() -> {
            try (var clsRef = FindClass(field.getDeclaringClass())) {
                var fidRef = (MemorySegment) JNIEnvFunctions.GetFieldId.invokeExact(
                        functions.GetFieldIDFp,
                        jniEnvPointer,
                        clsRef.ref(),
                        allocator.allocateFrom(field.getName()),
                        allocator.allocateFrom(PanamaUnsafeHelper.classToSig(field.getType()))
                );
                boolean isRef = false;
                long value = switch (field.getType().getName()) {
                    case "boolean" ->
                            (long) JNIEnvFunctions.GetBooleanField.invokeExact(functions.GetBooleanFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "byte" ->
                            (long) JNIEnvFunctions.GetByteField.invokeExact(functions.GetByteFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "char" ->
                            (long) JNIEnvFunctions.GetCharField.invokeExact(functions.GetCharFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "short" ->
                            (long) JNIEnvFunctions.GetShortField.invokeExact(functions.GetShortFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "int" ->
                            (long) JNIEnvFunctions.GetIntField.invokeExact(functions.GetIntFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "long" ->
                            (long) JNIEnvFunctions.GetLongField.invokeExact(functions.GetLongFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "float" ->
                            (long) JNIEnvFunctions.GetFloatField.invokeExact(functions.GetFloatFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    case "double" ->
                            (long) JNIEnvFunctions.GetDoubleField.invokeExact(functions.GetDoubleFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    default -> {
                        isRef = true;
                        yield (long) JNIEnvFunctions.GetObjectField.invokeExact(functions.GetObjectFieldFp, jniEnvPointer, jobject.ref(), fidRef);
                    }

                };
                return isRef ? new GlobalRef(this, MemorySegment.ofAddress(value)) : new GlobalRef(this, new JValue(value));
            }
        });
    }

    public void SetFieldByName(Field field, GlobalRef target, GlobalRef fieldValue) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("only support not static field");
        }
        throwable(() -> {
            try (var clsRef = FindClass(field.getDeclaringClass())) {
                var fidRef = (MemorySegment) JNIEnvFunctions.GetFieldId.invokeExact(
                        functions.GetFieldIDFp,
                        jniEnvPointer,
                        clsRef.ref(),
                        allocator.allocateFrom(field.getName()),
                        allocator.allocateFrom(PanamaUnsafeHelper.classToSig(field.getType()))
                );
                switch (field.getType().getName()) {
                    case "boolean" ->
                            JNIEnvFunctions.SetBooleanField.invokeExact(functions.SetBooleanFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getBoolean());
                    case "byte" ->
                            JNIEnvFunctions.SetByteField.invokeExact(functions.SetByteFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getByte());
                    case "char" ->
                            JNIEnvFunctions.SetCharField.invokeExact(functions.SetCharFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getChar());
                    case "short" ->
                            JNIEnvFunctions.SetShortField.invokeExact(functions.SetShortFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getShort());
                    case "int" ->
                            JNIEnvFunctions.SetIntField.invokeExact(functions.SetIntFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getInt());
                    case "long" ->
                            JNIEnvFunctions.SetLongField.invokeExact(functions.SetLongFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getLong());
                    case "float" ->
                            JNIEnvFunctions.SetFloatField.invokeExact(functions.SetFloatFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getFloat());
                    case "double" ->
                            JNIEnvFunctions.SetDoubleField.invokeExact(functions.SetDoubleFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.jValue.getDouble());
                    default ->
                            JNIEnvFunctions.SetObjectField.invokeExact(functions.SetObjectFieldFp, jniEnvPointer, target.ref(), fidRef, fieldValue.ref());
                }
            }
        });
    }

    private GlobalRef cstrToJstring(MemorySegment cstr) {
        return throwable(() -> new GlobalRef(this, (MemorySegment) NewStringPlatform.invokeExact(jniEnvPointer, cstr)));
    }

    public GlobalRef CallStaticMethodByName(Method method) {
        return CallStaticMethodByName(method, MemorySegment.NULL, "()" + PanamaUnsafeHelper.classToSig(method.getReturnType()));
    }

    public GlobalRef CallStaticMethodByName(Method method, MemorySegment jvalues) {
        String paramSig = Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(PanamaUnsafeHelper::classToSig)
                .collect(Collectors.joining());
        return CallStaticMethodByName(method, jvalues, "(" + paramSig + ")" + PanamaUnsafeHelper.classToSig(method.getReturnType()));
    }

    public GlobalRef CallStaticMethodByName(Method method, JValue... jvalues) {
        String paramSig = Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(PanamaUnsafeHelper::classToSig)
                .collect(Collectors.joining());
        long[] longs = new long[jvalues.length];
        for (int i = 0; i < jvalues.length; i++) {
            longs[i] = jvalues[i].getLong();
        }
        MemorySegment jValuesPtr = allocator.allocate(JValue.jvalueLayout, jvalues.length);
        jValuesPtr.copyFrom(MemorySegment.ofArray(longs));
        return CallStaticMethodByName(method, jValuesPtr, "(" + paramSig + ")" + PanamaUnsafeHelper.classToSig(method.getReturnType()));
    }

    /**
     * 只用于调用系统加载器加载的类中的静态方法
     * 原因在于：对应的jni实现里面先获取类加载器是查找vframe顶层的栈帧，拿到这个栈帧的owner，然后用这个owner所属的类加载器去找到对应的类
     * 而在这里顶层栈帧归属于MethodHandle,所以找到的类加载器是系统类加载器，所以只能调用系统类加载器加载的类
     *
     * @param method 需要调用的方法
     */
    public GlobalRef CallStaticMethodByName(Method method, MemorySegment jvalues, String sig) {
        if (method.getParameters().length * JValue.jvalueLayout.byteSize() != jvalues.byteSize()) {
            throw new IllegalArgumentException("jvalues size not match");
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("only support static method");
        }
        Class<?> ownerClass = method.getDeclaringClass();
        String methodName = method.getName();
        return throwable(() -> {
            //todo解析错误的问题
            try (GlobalRef jclassRef = FindClass(ownerClass)) {
                MemorySegment mid = (MemorySegment) JNIEnvFunctions.GetStaticMethodID_MH.invokeExact(functions.GetStaticMethodIDFp, jniEnvPointer, jclassRef.ref(), allocator.allocateFrom(methodName), allocator.allocateFrom(sig));
                boolean isRef = false;
                long jvalue = switch (method.getReturnType().getName()) {
                    case "void" -> {
                        JNIEnvFunctions.CallStaticVoidMethodA_MH.invokeExact(functions.CallStaticVoidMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                        yield 0L;
                    }
                    case "int" ->
                            (long) JNIEnvFunctions.CallStaticIntMethodA_MH.invokeExact(functions.CallStaticIntMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "boolean" ->
                            (long) JNIEnvFunctions.CallStaticBooleanMethodA_MH.invokeExact(functions.CallStaticBooleanMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "byte" ->
                            (long) JNIEnvFunctions.CallStaticByteMethodA_MH.invokeExact(functions.CallStaticByteMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "char" ->
                            (long) JNIEnvFunctions.CallStaticCharMethodA_MH.invokeExact(functions.CallStaticCharMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "short" ->
                            (long) JNIEnvFunctions.CallStaticShortMethodA_MH.invokeExact(functions.CallStaticShortMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "long" ->
                            (long) JNIEnvFunctions.CallStaticLongMethodA_MH.invokeExact(functions.CallStaticLongMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "float" ->
                            (long) JNIEnvFunctions.CallStaticFloatMethodA_MH.invokeExact(functions.CallStaticFloatMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    case "double" ->
                            (long) JNIEnvFunctions.CallStaticDoubleMethodA_MH.invokeExact(functions.CallStaticDoubleMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    default -> {
                        isRef = true;
                        yield (long) JNIEnvFunctions.CallStaticObjectMethodA_MH.invokeExact(functions.CallStaticObjectMethodAFp, jniEnvPointer, jclassRef.ref(), mid, jvalues);
                    }
                };
                return isRef ? new GlobalRef(this, MemorySegment.ofAddress(jvalue)) : new GlobalRef(this, new JValue(jvalue));
            }
        });
    }

    public GlobalRef CallMethodByName(Method method, MemorySegment jobject) {
        return CallMethodByName(method, jobject, MemorySegment.NULL, "()" + PanamaUnsafeHelper.classToSig(method.getReturnType()));
    }

    public GlobalRef CallMethodByName(Method method, MemorySegment jobject, MemorySegment jvalues) {
        String paramSig = Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(PanamaUnsafeHelper::classToSig)
                .collect(Collectors.joining());
        return CallMethodByName(method, jobject, jvalues, "(" + paramSig + ")" + PanamaUnsafeHelper.classToSig(method.getReturnType()));
    }

    public GlobalRef CallMethodByName(Method method, MemorySegment jobject, MemorySegment jvalues, String sig) {
        if (method.getParameters().length * JValue.jvalueLayout.byteSize() != jvalues.byteSize()) {
            throw new IllegalArgumentException("jvalues size not match");
        }
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("only support not static method");
        }
        String methodName = method.getName();
        return throwable(() -> {
            boolean isRef = false;
            MemorySegment clazz = (MemorySegment) JNIEnvFunctions.GetObjectClass_MH.invokeExact(functions.GetObjectClassFp, jniEnvPointer, jobject);
            try (GlobalRef ref = new GlobalRef(this, clazz);) {
                MemorySegment mid = (MemorySegment) JNIEnvFunctions.GetMethodID_MH.invokeExact(functions.GetMethodIDFp, jniEnvPointer, ref.ref(), allocator.allocateFrom(methodName), allocator.allocateFrom(sig));
                long returnValue = switch (method.getReturnType().getName()) {
                    case "void" -> {
                        JNIEnvFunctions.CallVoidMethodA_MH.invokeExact(functions.CallVoidMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                        yield 0L;
                    }
                    case "int" ->
                            (long) JNIEnvFunctions.CallIntMethodA_MH.invokeExact(functions.CallIntMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "boolean" ->
                            (long) JNIEnvFunctions.CallBooleanMethodA_MH.invokeExact(functions.CallBooleanMethodAFp, jniEnvPointer, mid, jvalues);
                    case "byte" ->
                            (long) JNIEnvFunctions.CallByteMethodA_MH.invokeExact(functions.CallByteMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "char" ->
                            (long) JNIEnvFunctions.CallCharMethodA_MH.invokeExact(functions.CallCharMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "short" ->
                            (long) JNIEnvFunctions.CallShortMethodA_MH.invokeExact(functions.CallShortMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "long" ->
                            (long) JNIEnvFunctions.CallLongMethodA_MH.invokeExact(functions.CallLongMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "float" ->
                            (long) JNIEnvFunctions.CallFloatMethodA_MH.invokeExact(functions.CallFloatMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    case "double" ->
                            (long) JNIEnvFunctions.CallDoubleMethodA_MH.invokeExact(functions.CallDoubleMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    default -> {
                        isRef = true;
                        yield (long) JNIEnvFunctions.CallObjectMethodA_MH.invokeExact(functions.CallObjectMethodAFp, jniEnvPointer, jobject, mid, jvalues);
                    }
                };
                return isRef ? new GlobalRef(this, MemorySegment.ofAddress(returnValue)) : new GlobalRef(this, new JValue(returnValue));
            }

        });
    }

    public Object jObjectToJavaObject(MemorySegment jobject) {
        return throwable(() -> {
            CallStaticMethodByName(JNIEnv.class.getDeclaredMethod("setSecret", Object.class), new JValue(jobject.address()));
            var res = jniToJava.get();
            jniToJava.remove();
            return res;
        });
    }

    public GlobalRef JavaObjectToJObject(Object o) {
        return throwable(() -> {
            setSecret(o);
            GlobalRef ref = CallStaticMethodByName(JNIEnv.class.getDeclaredMethod("getSecret"));
            jniToJava.remove();
            return ref;
        });
    }

    public GlobalRef newObject(Constructor ctr, JValue... jValues) {
        Parameter[] parameters = ctr.getParameters();
        if (parameters.length != jValues.length) {
            throw new IllegalArgumentException("jValues size not match");
        }
        String sig = Arrays.stream(parameters)
                .map(Parameter::getType)
                .map(PanamaUnsafeHelper::classToSig)
                .collect(Collectors.joining());
        String methodSig = "(" + sig + ")V";

        return throwable(() -> {
            MemorySegment jValuesPtr = allocator.allocate(JValue.jvalueLayout, jValues.length);
            for (int i = 0; i < jValues.length; i++) {
                jValuesPtr.set(ValueLayout.JAVA_LONG, i * JValue.jvalueLayout.byteSize(), jValues[i].getLong());
            }
            try (GlobalRef clsRef = FindClass(ctr.getDeclaringClass())) {
                MemorySegment mid = (MemorySegment) JNIEnvFunctions.GetMethodID_MH.invokeExact(functions.GetMethodIDFp, jniEnvPointer, clsRef.ref(), allocator.allocateFrom("<init>"), allocator.allocateFrom(methodSig));
                MemorySegment newobject = (MemorySegment) JNIEnvFunctions.NewObject_MH.invokeExact(functions.NewObjectAFp, jniEnvPointer, clsRef.ref(), mid, jValuesPtr);
                return new GlobalRef(this, newobject);
            }
        });
    }

    private MemorySegment initJniEnv() {
        try {
            return ((MemorySegment) GET_JNIENV_MH.invokeExact(MAIN_VM_Pointer, JNI_VERSION))
                    .reinterpret(Long.MAX_VALUE);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
