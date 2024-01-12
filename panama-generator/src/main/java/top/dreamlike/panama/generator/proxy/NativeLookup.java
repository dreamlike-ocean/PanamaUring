package top.dreamlike.panama.generator.proxy;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Optional;

class NativeLookup implements SymbolLookup {

    public static final MethodHandle AllocateErrorBuffer_MH;
    public static final MethodHandle FILL_ERROR_CODE_VOID_MH;
    public static final MethodHandle FILL_ERROR_CODE_BYTE_MH;
    public static final MethodHandle FILL_ERROR_CODE_SHORT_MH;
    public static final MethodHandle FILL_ERROR_CODE_INT_MH;
    public static final MethodHandle FILL_ERROR_CODE_LONG_MH;
    public static final MethodHandle FILL_ERROR_CODE_FLOAT_MH;
    public static final MethodHandle FILL_ERROR_CODE_DOUBLE_MH;
    public static final MethodHandle FILL_ERROR_CODE_BOOLEAN_MH;
    public static final MethodHandle FILL_ERROR_CODE_CHAR_MH;
    public static final MethodHandle FILL_ERROR_CODE_ADDRESS_MH;
    private static final VarHandle errorHandle;

    //sb java 只能枚举全部原始类型了
    static ThreadLocal<MemorySegment> errorBuffer = new ThreadLocal<>();

    static {
        try {
            AllocateErrorBuffer_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "allocateErrorBuffer", MethodType.methodType(MemorySegment.class));
            errorHandle = Linker.Option.captureStateLayout()
                    .varHandle(MemoryLayout.PathElement.groupElement("errno"));
            FILL_ERROR_CODE_VOID_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorVoid", MethodType.methodType(void.class));
            FILL_ERROR_CODE_BYTE_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorByte", MethodType.methodType(byte.class, byte.class));
            FILL_ERROR_CODE_SHORT_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorShort", MethodType.methodType(short.class, short.class));
            FILL_ERROR_CODE_INT_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorInt", MethodType.methodType(int.class, int.class));
            FILL_ERROR_CODE_LONG_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorLong", MethodType.methodType(long.class, long.class));
            FILL_ERROR_CODE_FLOAT_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorFloat", MethodType.methodType(float.class, float.class));
            FILL_ERROR_CODE_DOUBLE_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorDouble", MethodType.methodType(double.class, double.class));
            FILL_ERROR_CODE_BOOLEAN_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorBoolean", MethodType.methodType(boolean.class, boolean.class));
            FILL_ERROR_CODE_CHAR_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorChar", MethodType.methodType(char.class, char.class));
            FILL_ERROR_CODE_ADDRESS_MH = MethodHandles.lookup()
                    .findStatic(NativeLookup.class, "fillTLErrorAddress", MethodType.methodType(MemorySegment.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment allocateErrorBuffer() {
        SegmentAllocator allocator = MemoryLifetimeScope.currentAllocator.orElseThrow(() -> new IllegalStateException("please active MemoryLifetimeScope first!"));
        StructLayout structLayout = Linker.Option.captureStateLayout();
        MemorySegment buffer = allocator.allocate(structLayout);
        errorBuffer.set(buffer);
        return buffer;
    }

    public static MethodHandle fillErrorNoAfterReturn(MethodHandle methodHandle) {
        Class<?> returnType = methodHandle.type().returnType();
        return switch (returnType) {
            case Class c when c == int.class -> MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_INT_MH);
            case Class c when c == long.class -> MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_LONG_MH);
            case Class c when c == short.class ->
                    MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_SHORT_MH);
            case Class c when c == char.class -> MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_CHAR_MH);
            case Class c when c == float.class ->
                    MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_FLOAT_MH);
            case Class c when c == double.class ->
                    MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_DOUBLE_MH);
            case Class c when c == boolean.class ->
                    MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_BOOLEAN_MH);
            case Class c when c == void.class -> MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_VOID_MH);
            default -> MethodHandles.filterReturnValue(methodHandle, FILL_ERROR_CODE_ADDRESS_MH);
        };
    }

    public static void fillTLErrorVoid() {
        MemorySegment errorSegment = errorBuffer.get();
        int errorCode = (int) errorHandle.get(errorSegment);
        ErrorNo.error.set(errorCode);
    }

    public static byte fillTLErrorByte(byte returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static int fillTLErrorInt(int returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static long fillTLErrorLong(long returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static float fillTLErrorFloat(float returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static double fillTLErrorDouble(double returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static boolean fillTLErrorBoolean(boolean returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static char fillTLErrorChar(char returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static short fillTLErrorShort(short returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    public static MemorySegment fillTLErrorAddress(MemorySegment returnValue) {
        fillTLErrorVoid();
        return returnValue;
    }

    @Override
    public Optional<MemorySegment> find(String name) {
        return SymbolLookup.loaderLookup()
                .find(name)
                .or(() -> Linker.nativeLinker().defaultLookup().find(name));
    }

    public MethodHandle downcallHandle(String name, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        return find(name)
                .map(functionAddr -> Linker.nativeLinker().downcallHandle(functionAddr, functionDescriptor, options))
                .orElseThrow(() -> new IllegalArgumentException(STR. "cant link \{ name }" ));
    }

    public static MemoryLayout primitiveMapToMemoryLayout(Class source) {
        return switch (source) {
            case Class c when c == int.class -> ValueLayout.JAVA_INT;
            case Class c when c == long.class -> ValueLayout.JAVA_LONG;
            case Class c when c == double.class -> ValueLayout.JAVA_DOUBLE;
            case Class c when c == float.class -> ValueLayout.JAVA_FLOAT;
            case Class c when c == byte.class -> ValueLayout.JAVA_BYTE;
            case Class c when c == boolean.class -> ValueLayout.JAVA_BOOLEAN;
            case Class c when c == char.class -> ValueLayout.JAVA_CHAR;
            case Class c when c == short.class -> ValueLayout.JAVA_SHORT;
//            case Class c when NativeStructEnhanceMark.class.isAssignableFrom(c) -> ValueLayout.ADDRESS;
            default -> null;
        };
    }

}
