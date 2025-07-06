package top.dreamlike.panama.uring.helper.unsafe;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

class JNIEnvFunctions {
    final MemorySegment jniEnvPointer;

    private static final long ADDRESS_SIZE = ValueLayout.ADDRESS.byteSize();

    final static MethodHandle FindClassMH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jclass*/ValueLayout.ADDRESS,
            /*JNIEnv *env */ValueLayout.ADDRESS,
            /*const char *name*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle NewGlobalRef_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    final static MethodHandle DeleteGlobalRef_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    final static MethodHandle GetStaticFieldID_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*const char *name*/ ValueLayout.ADDRESS,
            /*const char *sig*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticObjectField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticBooleanField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jboolean*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticByteField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jbyte*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticCharField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jchar*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticShortField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jshort*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticIntField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jint*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticLongField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jlong*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticFloatField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jfloat*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetStaticDoubleField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            /*jdouble*/ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle SetStaticObjectField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jobject value*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle SetStaticBooleanField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jboolean value*/ ValueLayout.JAVA_BOOLEAN
    ));

    final static MethodHandle SetStaticByteField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jbyte value*/ ValueLayout.JAVA_BYTE
    ));

    final static MethodHandle SetStaticCharField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jchar value*/ ValueLayout.JAVA_CHAR
    ));

    final static MethodHandle SetStaticShortField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jshort value*/ ValueLayout.JAVA_SHORT
    ));

    final static MethodHandle SetStaticIntField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jint value*/ ValueLayout.JAVA_INT
    ));

    final static MethodHandle SetStaticLongField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jlong value*/ ValueLayout.JAVA_LONG
    ));

    final static MethodHandle SetStaticFloatField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jfloat value*/ ValueLayout.JAVA_FLOAT
    ));

    final static MethodHandle SetStaticDoubleField_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jdouble value*/ ValueLayout.JAVA_DOUBLE
    ));

    final static MethodHandle GetStaticMethodID_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*const char *name*/ ValueLayout.ADDRESS,
            /*const char *sig*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticVoidMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticBooleanMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticByteMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticCharMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticShortMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticIntMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticLongMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticFloatMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticDoubleMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallStaticObjectMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetMethodID_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*const char *name*/ ValueLayout.ADDRESS,
            /*const char *sig*/ ValueLayout.ADDRESS
    ));


    final static MethodHandle CallObjectMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallBooleanMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallByteMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallCharMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallShortMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallIntMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallLongMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallFloatMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallDoubleMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle CallVoidMethodA_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetObjectClass_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle NewObject_MH = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*jmethodID methodID*/ ValueLayout.ADDRESS,
            /*jvalue *args*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetFieldId = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.ADDRESS,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jclass clazz*/ ValueLayout.ADDRESS,
            /*const char *name*/ ValueLayout.ADDRESS,
            /*const char *sig*/ ValueLayout.ADDRESS
    ));

final static MethodHandle GetObjectField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetBooleanField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetByteField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetCharField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetShortField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetIntField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetLongField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetFloatField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle GetDoubleField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle SetObjectField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jobject value*/ ValueLayout.ADDRESS
    ));

    final static MethodHandle SetBooleanField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jboolean value*/ ValueLayout.JAVA_BOOLEAN
    ));

    final static MethodHandle SetByteField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jbyte value*/ ValueLayout.JAVA_BYTE
    ));

    final static MethodHandle SetCharField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jchar value*/ ValueLayout.JAVA_CHAR
    ));

    final static MethodHandle SetShortField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jshort value*/ ValueLayout.JAVA_SHORT
    ));

    final static MethodHandle SetIntField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jint value*/ ValueLayout.JAVA_INT
    ));

    final static MethodHandle SetLongField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jlong value*/ ValueLayout.JAVA_LONG
    ));

    final static MethodHandle SetFloatField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jfloat value*/ ValueLayout.JAVA_FLOAT
    ));

    final static MethodHandle SetDoubleField = Linker.nativeLinker().downcallHandle(FunctionDescriptor.ofVoid(
            /*JNIEnv *env */ ValueLayout.ADDRESS,
            /*jobject obj*/ ValueLayout.ADDRESS,
            /*jfieldID fieldID*/ ValueLayout.ADDRESS,
            /*jdouble value*/ ValueLayout.JAVA_DOUBLE
    ));

    JNIEnvFunctions(MemorySegment jniEnvPointer) {
        this.jniEnvPointer = jniEnvPointer;
        int i = 4;
        MemorySegment functions = jniEnvPointer.get(ValueLayout.ADDRESS, 0).reinterpret(Long.MAX_VALUE);
        GetVersionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        DefineClassFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        FindClassFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        FromReflectedMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        FromReflectedFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ToReflectedMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetSuperclassFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        IsAssignableFromFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ToReflectedFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ThrowFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ThrowNewFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ExceptionOccurredFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ExceptionDescribeFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ExceptionClearFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        FatalErrorFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        PushLocalFrameFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        PopLocalFrameFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewGlobalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        DeleteGlobalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        DeleteLocalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        IsSameObjectFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewLocalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        EnsureLocalCapacityFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        AllocObjectFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewObjectFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewObjectVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewObjectAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetObjectClassFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        IsInstanceOfFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetMethodIDFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallObjectMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallObjectMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallObjectMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallBooleanMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallBooleanMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallBooleanMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallByteMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallByteMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallByteMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallCharMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallCharMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallCharMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallShortMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallShortMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallShortMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallIntMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallIntMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallIntMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallLongMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallLongMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallLongMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallFloatMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallFloatMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallFloatMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallDoubleMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallDoubleMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallDoubleMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallVoidMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallVoidMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallVoidMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualObjectMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualObjectMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualObjectMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualBooleanMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualBooleanMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualBooleanMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualByteMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualByteMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualByteMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualCharMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualCharMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualCharMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualShortMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualShortMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualShortMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualIntMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualIntMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualIntMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualLongMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualLongMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualLongMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualFloatMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualFloatMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualFloatMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualDoubleMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualDoubleMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualDoubleMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualVoidMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualVoidMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallNonvirtualVoidMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetFieldIDFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetObjectFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetBooleanFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetByteFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetCharFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetShortFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetIntFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetLongFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetFloatFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetDoubleFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetObjectFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetBooleanFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetByteFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetCharFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetShortFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetIntFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetLongFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetFloatFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetDoubleFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticMethodIDFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticObjectMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticObjectMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticObjectMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticBooleanMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticBooleanMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticBooleanMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticByteMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticByteMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticByteMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticCharMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticCharMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticCharMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticShortMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticShortMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticShortMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticIntMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticIntMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticIntMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticLongMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticLongMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticLongMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticFloatMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticFloatMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticFloatMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticDoubleMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticDoubleMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticDoubleMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticVoidMethodFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticVoidMethodVFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        CallStaticVoidMethodAFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticFieldIDFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticObjectFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticBooleanFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticByteFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticCharFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticShortFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticIntFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticLongFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticFloatFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStaticDoubleFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticObjectFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticBooleanFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticByteFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticCharFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticShortFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticIntFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticLongFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticFloatFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetStaticDoubleFieldFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewStringFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringLengthFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringCharsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseStringCharsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewStringUTFFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringUTFLengthFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringUTFCharsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseStringUTFCharsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetArrayLengthFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewObjectArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetObjectArrayElementFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetObjectArrayElementFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewBooleanArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewByteArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewCharArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewShortArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewIntArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewLongArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewFloatArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewDoubleArrayFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetBooleanArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetByteArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetCharArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetShortArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetIntArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetLongArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetFloatArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetDoubleArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseBooleanArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseByteArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseCharArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseShortArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseIntArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseLongArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseFloatArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseDoubleArrayElementsFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetBooleanArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetByteArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetCharArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetShortArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetIntArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetLongArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetFloatArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetDoubleArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetBooleanArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetByteArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetCharArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetShortArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetIntArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetLongArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetFloatArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        SetDoubleArrayRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        RegisterNativesFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        UnregisterNativesFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        MonitorEnterFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        MonitorExitFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetJavaVMFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringUTFRegionFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetPrimitiveArrayCriticalFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleasePrimitiveArrayCriticalFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetStringCriticalFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ReleaseStringCriticalFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewWeakGlobalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        DeleteWeakGlobalRefFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        ExceptionCheckFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        NewDirectByteBufferFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetDirectBufferAddressFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetDirectBufferCapacityFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetObjectRefTypeFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        GetModuleFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
        IsVirtualThreadFp = functions.get(ValueLayout.ADDRESS, ADDRESS_SIZE * i++);
    }


    final MemorySegment GetVersionFp;
    final MemorySegment DefineClassFp;
    final MemorySegment FindClassFp;
    final MemorySegment FromReflectedMethodFp;
    final MemorySegment FromReflectedFieldFp;
    final MemorySegment ToReflectedMethodFp;
    final MemorySegment GetSuperclassFp;
    final MemorySegment IsAssignableFromFp;
    final MemorySegment ToReflectedFieldFp;
    final MemorySegment ThrowFp;
    final MemorySegment ThrowNewFp;
    final MemorySegment ExceptionOccurredFp;
    final MemorySegment ExceptionDescribeFp;
    final MemorySegment ExceptionClearFp;
    final MemorySegment FatalErrorFp;
    final MemorySegment PushLocalFrameFp;
    final MemorySegment PopLocalFrameFp;
    final MemorySegment NewGlobalRefFp;
    final MemorySegment DeleteGlobalRefFp;
    final MemorySegment DeleteLocalRefFp;
    final MemorySegment IsSameObjectFp;
    final MemorySegment NewLocalRefFp;
    final MemorySegment EnsureLocalCapacityFp;
    final MemorySegment AllocObjectFp;
    final MemorySegment NewObjectFp;
    final MemorySegment NewObjectVFp;
    final MemorySegment NewObjectAFp;
    final MemorySegment GetObjectClassFp;
    final MemorySegment IsInstanceOfFp;
    final MemorySegment GetMethodIDFp;
    final MemorySegment CallObjectMethodFp;
    final MemorySegment CallObjectMethodVFp;
    final MemorySegment CallObjectMethodAFp;
    final MemorySegment CallBooleanMethodFp;
    final MemorySegment CallBooleanMethodVFp;
    final MemorySegment CallBooleanMethodAFp;
    final MemorySegment CallByteMethodFp;
    final MemorySegment CallByteMethodVFp;
    final MemorySegment CallByteMethodAFp;
    final MemorySegment CallCharMethodFp;
    final MemorySegment CallCharMethodVFp;
    final MemorySegment CallCharMethodAFp;
    final MemorySegment CallShortMethodFp;
    final MemorySegment CallShortMethodVFp;
    final MemorySegment CallShortMethodAFp;
    final MemorySegment CallIntMethodFp;
    final MemorySegment CallIntMethodVFp;
    final MemorySegment CallIntMethodAFp;
    final MemorySegment CallLongMethodFp;
    final MemorySegment CallLongMethodVFp;
    final MemorySegment CallLongMethodAFp;
    final MemorySegment CallFloatMethodFp;
    final MemorySegment CallFloatMethodVFp;
    final MemorySegment CallFloatMethodAFp;
    final MemorySegment CallDoubleMethodFp;
    final MemorySegment CallDoubleMethodVFp;
    final MemorySegment CallDoubleMethodAFp;
    final MemorySegment CallVoidMethodFp;
    final MemorySegment CallVoidMethodVFp;
    final MemorySegment CallVoidMethodAFp;
    final MemorySegment CallNonvirtualObjectMethodFp;
    final MemorySegment CallNonvirtualObjectMethodVFp;
    final MemorySegment CallNonvirtualObjectMethodAFp;
    final MemorySegment CallNonvirtualBooleanMethodFp;
    final MemorySegment CallNonvirtualBooleanMethodVFp;
    final MemorySegment CallNonvirtualBooleanMethodAFp;
    final MemorySegment CallNonvirtualByteMethodFp;
    final MemorySegment CallNonvirtualByteMethodVFp;
    final MemorySegment CallNonvirtualByteMethodAFp;
    final MemorySegment CallNonvirtualCharMethodFp;
    final MemorySegment CallNonvirtualCharMethodVFp;
    final MemorySegment CallNonvirtualCharMethodAFp;
    final MemorySegment CallNonvirtualShortMethodFp;
    final MemorySegment CallNonvirtualShortMethodVFp;
    final MemorySegment CallNonvirtualShortMethodAFp;
    final MemorySegment CallNonvirtualIntMethodFp;
    final MemorySegment CallNonvirtualIntMethodVFp;
    final MemorySegment CallNonvirtualIntMethodAFp;
    final MemorySegment CallNonvirtualLongMethodFp;
    final MemorySegment CallNonvirtualLongMethodVFp;
    final MemorySegment CallNonvirtualLongMethodAFp;
    final MemorySegment CallNonvirtualFloatMethodFp;
    final MemorySegment CallNonvirtualFloatMethodVFp;
    final MemorySegment CallNonvirtualFloatMethodAFp;
    final MemorySegment CallNonvirtualDoubleMethodFp;
    final MemorySegment CallNonvirtualDoubleMethodVFp;
    final MemorySegment CallNonvirtualDoubleMethodAFp;
    final MemorySegment CallNonvirtualVoidMethodFp;
    final MemorySegment CallNonvirtualVoidMethodVFp;
    final MemorySegment CallNonvirtualVoidMethodAFp;
    final MemorySegment GetFieldIDFp;
    final MemorySegment GetObjectFieldFp;
    final MemorySegment GetBooleanFieldFp;
    final MemorySegment GetByteFieldFp;
    final MemorySegment GetCharFieldFp;
    final MemorySegment GetShortFieldFp;
    final MemorySegment GetIntFieldFp;
    final MemorySegment GetLongFieldFp;
    final MemorySegment GetFloatFieldFp;
    final MemorySegment GetDoubleFieldFp;
    final MemorySegment SetObjectFieldFp;
    final MemorySegment SetBooleanFieldFp;
    final MemorySegment SetByteFieldFp;
    final MemorySegment SetCharFieldFp;
    final MemorySegment SetShortFieldFp;
    final MemorySegment SetIntFieldFp;
    final MemorySegment SetLongFieldFp;
    final MemorySegment SetFloatFieldFp;
    final MemorySegment SetDoubleFieldFp;
    final MemorySegment GetStaticMethodIDFp;
    final MemorySegment CallStaticObjectMethodFp;
    final MemorySegment CallStaticObjectMethodVFp;
    final MemorySegment CallStaticObjectMethodAFp;
    final MemorySegment CallStaticBooleanMethodFp;
    final MemorySegment CallStaticBooleanMethodVFp;
    final MemorySegment CallStaticBooleanMethodAFp;
    final MemorySegment CallStaticByteMethodFp;
    final MemorySegment CallStaticByteMethodVFp;
    final MemorySegment CallStaticByteMethodAFp;
    final MemorySegment CallStaticCharMethodFp;
    final MemorySegment CallStaticCharMethodVFp;
    final MemorySegment CallStaticCharMethodAFp;
    final MemorySegment CallStaticShortMethodFp;
    final MemorySegment CallStaticShortMethodVFp;
    final MemorySegment CallStaticShortMethodAFp;
    final MemorySegment CallStaticIntMethodFp;
    final MemorySegment CallStaticIntMethodVFp;
    final MemorySegment CallStaticIntMethodAFp;
    final MemorySegment CallStaticLongMethodFp;
    final MemorySegment CallStaticLongMethodVFp;
    final MemorySegment CallStaticLongMethodAFp;
    final MemorySegment CallStaticFloatMethodFp;
    final MemorySegment CallStaticFloatMethodVFp;
    final MemorySegment CallStaticFloatMethodAFp;
    final MemorySegment CallStaticDoubleMethodFp;
    final MemorySegment CallStaticDoubleMethodVFp;
    final MemorySegment CallStaticDoubleMethodAFp;
    final MemorySegment CallStaticVoidMethodFp;
    final MemorySegment CallStaticVoidMethodVFp;
    final MemorySegment CallStaticVoidMethodAFp;
    final MemorySegment GetStaticFieldIDFp;
    final MemorySegment GetStaticObjectFieldFp;
    final MemorySegment GetStaticBooleanFieldFp;
    final MemorySegment GetStaticByteFieldFp;
    final MemorySegment GetStaticCharFieldFp;
    final MemorySegment GetStaticShortFieldFp;
    final MemorySegment GetStaticIntFieldFp;
    final MemorySegment GetStaticLongFieldFp;
    final MemorySegment GetStaticFloatFieldFp;
    final MemorySegment GetStaticDoubleFieldFp;
    final MemorySegment SetStaticObjectFieldFp;
    final MemorySegment SetStaticBooleanFieldFp;
    final MemorySegment SetStaticByteFieldFp;
    final MemorySegment SetStaticCharFieldFp;
    final MemorySegment SetStaticShortFieldFp;
    final MemorySegment SetStaticIntFieldFp;
    final MemorySegment SetStaticLongFieldFp;
    final MemorySegment SetStaticFloatFieldFp;
    final MemorySegment SetStaticDoubleFieldFp;
    final MemorySegment NewStringFp;
    final MemorySegment GetStringLengthFp;
    final MemorySegment GetStringCharsFp;
    final MemorySegment ReleaseStringCharsFp;
    final MemorySegment NewStringUTFFp;
    final MemorySegment GetStringUTFLengthFp;
    final MemorySegment GetStringUTFCharsFp;
    final MemorySegment ReleaseStringUTFCharsFp;
    final MemorySegment GetArrayLengthFp;
    final MemorySegment NewObjectArrayFp;
    final MemorySegment GetObjectArrayElementFp;
    final MemorySegment SetObjectArrayElementFp;
    final MemorySegment NewBooleanArrayFp;
    final MemorySegment NewByteArrayFp;
    final MemorySegment NewCharArrayFp;
    final MemorySegment NewShortArrayFp;
    final MemorySegment NewIntArrayFp;
    final MemorySegment NewLongArrayFp;
    final MemorySegment NewFloatArrayFp;
    final MemorySegment NewDoubleArrayFp;
    final MemorySegment GetBooleanArrayElementsFp;
    final MemorySegment GetByteArrayElementsFp;
    final MemorySegment GetCharArrayElementsFp;
    final MemorySegment GetShortArrayElementsFp;
    final MemorySegment GetIntArrayElementsFp;
    final MemorySegment GetLongArrayElementsFp;
    final MemorySegment GetFloatArrayElementsFp;
    final MemorySegment GetDoubleArrayElementsFp;
    final MemorySegment ReleaseBooleanArrayElementsFp;
    final MemorySegment ReleaseByteArrayElementsFp;
    final MemorySegment ReleaseCharArrayElementsFp;
    final MemorySegment ReleaseShortArrayElementsFp;
    final MemorySegment ReleaseIntArrayElementsFp;
    final MemorySegment ReleaseLongArrayElementsFp;
    final MemorySegment ReleaseFloatArrayElementsFp;
    final MemorySegment ReleaseDoubleArrayElementsFp;
    final MemorySegment GetBooleanArrayRegionFp;
    final MemorySegment GetByteArrayRegionFp;
    final MemorySegment GetCharArrayRegionFp;
    final MemorySegment GetShortArrayRegionFp;
    final MemorySegment GetIntArrayRegionFp;
    final MemorySegment GetLongArrayRegionFp;
    final MemorySegment GetFloatArrayRegionFp;
    final MemorySegment GetDoubleArrayRegionFp;
    final MemorySegment SetBooleanArrayRegionFp;
    final MemorySegment SetByteArrayRegionFp;
    final MemorySegment SetCharArrayRegionFp;
    final MemorySegment SetShortArrayRegionFp;
    final MemorySegment SetIntArrayRegionFp;
    final MemorySegment SetLongArrayRegionFp;
    final MemorySegment SetFloatArrayRegionFp;
    final MemorySegment SetDoubleArrayRegionFp;
    final MemorySegment RegisterNativesFp;
    final MemorySegment UnregisterNativesFp;
    final MemorySegment MonitorEnterFp;
    final MemorySegment MonitorExitFp;
    final MemorySegment GetJavaVMFp;
    final MemorySegment GetStringRegionFp;
    final MemorySegment GetStringUTFRegionFp;
    final MemorySegment GetPrimitiveArrayCriticalFp;
    final MemorySegment ReleasePrimitiveArrayCriticalFp;
    final MemorySegment GetStringCriticalFp;
    final MemorySegment ReleaseStringCriticalFp;
    final MemorySegment NewWeakGlobalRefFp;
    final MemorySegment DeleteWeakGlobalRefFp;
    final MemorySegment ExceptionCheckFp;
    final MemorySegment NewDirectByteBufferFp;
    final MemorySegment GetDirectBufferAddressFp;
    final MemorySegment GetDirectBufferCapacityFp;
    final MemorySegment GetObjectRefTypeFp;
    final MemorySegment GetModuleFp;
    final MemorySegment IsVirtualThreadFp;

}


