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

    public static final MethodHandle CURRENT_ALLOCTOR_MH;

    private static final MethodHandle MEMORY_SEGMENT_HEAP_INT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_LONG_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_CHAR_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_FLOAT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_DOUBLE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_BYTE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_SHORT_MH;

    public static final MethodHandle CSTR_TOSTRING_MH;
    public static final MethodHandle JAVASTR_CSTR_MH;

    private static final VarHandle ERROR_HANDLE;
    private static final VarHandle GetLastError_HANDLE;
    private static final VarHandle WSAGetLastError_HANDLE;

    //sb java 只能枚举全部原始类型了
    static ThreadLocal<MemorySegment> errorBuffer = new ThreadLocal<>();

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            AllocateErrorBuffer_MH = lookup
                    .findStatic(NativeLookup.class, "allocateErrorBuffer", MethodType.methodType(MemorySegment.class));
            CURRENT_ALLOCTOR_MH = lookup
                    .findStatic(NativeLookup.class, "currentAllocator", MethodType.methodType(SegmentAllocator.class));
            StructLayout structLayout = Linker.Option.captureStateLayout();
            ERROR_HANDLE = MethodHandles.insertCoordinates(
                    structLayout.varHandle(MemoryLayout.PathElement.groupElement("errno")),
                    1, 0
            );

            // windows有三种
            // 参考https://github.com/openjdk/jdk/blob/e7c7892b9f0fcee37495cce312fdd67dc800f9c9/src/hotspot/share/prims/downcallLinker.cpp#L34
            if (structLayout.memberLayouts().size() > 1) {
                GetLastError_HANDLE = MethodHandles.insertCoordinates(
                        structLayout.varHandle(MemoryLayout.PathElement.groupElement("GetLastError")),
                        1, 0
                );
                WSAGetLastError_HANDLE = MethodHandles.insertCoordinates(
                        structLayout.varHandle(MemoryLayout.PathElement.groupElement("WSAGetLastError")),
                        1, 0
                );
            } else {
                GetLastError_HANDLE = null;
                WSAGetLastError_HANDLE = null;
            }


            FILL_ERROR_CODE_VOID_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorVoid", MethodType.methodType(void.class));
            FILL_ERROR_CODE_BYTE_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorByte", MethodType.methodType(byte.class, byte.class));
            FILL_ERROR_CODE_SHORT_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorShort", MethodType.methodType(short.class, short.class));
            FILL_ERROR_CODE_INT_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorInt", MethodType.methodType(int.class, int.class));
            FILL_ERROR_CODE_LONG_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorLong", MethodType.methodType(long.class, long.class));
            FILL_ERROR_CODE_FLOAT_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorFloat", MethodType.methodType(float.class, float.class));
            FILL_ERROR_CODE_DOUBLE_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorDouble", MethodType.methodType(double.class, double.class));
            FILL_ERROR_CODE_BOOLEAN_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorBoolean", MethodType.methodType(boolean.class, boolean.class));
            FILL_ERROR_CODE_CHAR_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorChar", MethodType.methodType(char.class, char.class));
            FILL_ERROR_CODE_ADDRESS_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorAddress", MethodType.methodType(MemorySegment.class, MemorySegment.class));

            MEMORY_SEGMENT_HEAP_INT_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, int[].class));
            MEMORY_SEGMENT_HEAP_LONG_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, long[].class));
            MEMORY_SEGMENT_HEAP_CHAR_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, char[].class));
            MEMORY_SEGMENT_HEAP_FLOAT_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, float[].class));
            MEMORY_SEGMENT_HEAP_DOUBLE_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, double[].class));
            MEMORY_SEGMENT_HEAP_BYTE_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, byte[].class));
            MEMORY_SEGMENT_HEAP_SHORT_MH = lookup.findStatic(MemorySegment.class, "ofArray", MethodType.methodType(MemorySegment.class, short[].class));

            CSTR_TOSTRING_MH = MethodHandles.lookup().findStatic(NativeLookup.class, "ctrToJavaString", MethodType.methodType(String.class, MemorySegment.class));
            JAVASTR_CSTR_MH = MethodHandles.lookup().findStatic(NativeLookup.class, "toCStr", MethodType.methodType(MemorySegment.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment allocateErrorBuffer() {
        MemoryLifetimeScope memoryLifetimeScope = MemoryLifetimeScope.currentScope();
        StructLayout errorNoLayout = Linker.Option.captureStateLayout();
        MemorySegment buffer = memoryLifetimeScope.allocator.allocate(errorNoLayout);
        errorBuffer.set(buffer);
        return buffer;
    }

    public static SegmentAllocator currentAllocator() {
        return MemoryLifetimeScope.currentScope().allocator;
    }

    public static MemorySegment toCStr(String javaString) {
        MemoryLifetimeScope memoryLifetimeScope = MemoryLifetimeScope.currentScope();
        return memoryLifetimeScope.allocator.allocateFrom(javaString);
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

    public static MethodHandle heapAccessMH(Class primitiveType) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("primitiveType must be a primitive type");
        }
        return switch (primitiveType) {
            case Class c when c == int.class -> MEMORY_SEGMENT_HEAP_INT_MH;
            case Class c when c == long.class -> MEMORY_SEGMENT_HEAP_LONG_MH;
            case Class c when c == short.class -> MEMORY_SEGMENT_HEAP_SHORT_MH;
            case Class c when c == char.class -> MEMORY_SEGMENT_HEAP_CHAR_MH;
            case Class c when c == float.class -> MEMORY_SEGMENT_HEAP_FLOAT_MH;
            case Class c when c == double.class -> MEMORY_SEGMENT_HEAP_DOUBLE_MH;
            case Class c when c == byte.class -> MEMORY_SEGMENT_HEAP_BYTE_MH;
            default -> throw new IllegalArgumentException("primitiveType must be a primitive type");
        };
    }

    public static void fillTLErrorVoid() {
        MemorySegment errorSegment = errorBuffer.get();
        ErrorNo.CapturedErrorState capturedErrorState;
        if (GetLastError_HANDLE == null && WSAGetLastError_HANDLE == null) {
            capturedErrorState = fillPosixError(errorSegment);
        } else {
            capturedErrorState = fillWindowsError(errorSegment);
        }
        ErrorNo.error.set(capturedErrorState.errno());
        ErrorNo.capturedError.set(capturedErrorState);
    }

    private static ErrorNo.PosixCapturedError fillPosixError(MemorySegment errorNoBuffer) {
        return new ErrorNo.PosixCapturedError(((int) ERROR_HANDLE.get(errorNoBuffer)));
    }

    private static ErrorNo.WindowsCapturedError fillWindowsError(MemorySegment errorNoBuffer) {
        return new ErrorNo.WindowsCapturedError(
                (int) ERROR_HANDLE.get(errorNoBuffer),
                (int) GetLastError_HANDLE.get(errorNoBuffer),
                (int) WSAGetLastError_HANDLE.get(errorNoBuffer)
        );
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

    public static String ctrToJavaString(MemorySegment memorySegment) {
        return memorySegment
                .reinterpret(Long.MAX_VALUE)
                .getString(0);
    }

    @Override
    public Optional<MemorySegment> find(String name) {
        return SymbolLookup.loaderLookup()
                .find(name)
                .or(() -> Linker.nativeLinker().defaultLookup().find(name));
    }

    public MemorySegment findOrException(String name) {
        return find(name)
                .orElseThrow(() -> new IllegalArgumentException("cant link to " + name));
    }

    static Linker.Option guessErrorNoOption() {
        return Linker.Option.captureCallState(Linker.Option.captureStateLayout().memberLayouts().stream()
                .map(MemoryLayout::name)
                .flatMap(Optional::stream)
                .toArray(String[]::new)
        );
    }
}
