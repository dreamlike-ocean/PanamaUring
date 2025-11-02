package top.dreamlike.panama.generator.proxy;


import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class NativeLookup implements SymbolLookup {

    public static final MethodHandle AllocateErrorBuffer_MH;
    public static final MethodHandle FILL_ERROR_CODE_AUTO_MH;
    public static final MethodHandle FILL_ERROR_CODE_BY_TYPES_MH;

    public static final MethodHandle CURRENT_ALLOCTOR_MH;
    public static final MethodHandle CSTR_TOSTRING_MH;
    public static final MethodHandle JAVASTR_CSTR_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_INT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_LONG_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_CHAR_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_FLOAT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_DOUBLE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_BYTE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_SHORT_MH;
    private static final VarHandle ERROR_HANDLE;
    private static final VarHandle GetLastError_HANDLE;
    private static final VarHandle WSAGetLastError_HANDLE;

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


            FILL_ERROR_CODE_AUTO_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorAuto", MethodType.methodType(void.class));
            FILL_ERROR_CODE_BY_TYPES_MH = lookup
                    .findStatic(NativeLookup.class, "fillTLErrorByTypes", MethodType.methodType(void.class, ErrorNo.ErrorNoType[].class));
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

    public static MethodHandle fillErrorAfterReturn(MethodHandle downcallMH) {
        Class<?> returnType = downcallMH.type().returnType();
        if (returnType == void.class) {
            return MethodHandles.filterReturnValue(downcallMH, FILL_ERROR_CODE_AUTO_MH);
        }

        // (returnType) -> void
        MethodHandle errorSetMH = MethodHandles.dropArguments(FILL_ERROR_CODE_AUTO_MH, 0, returnType);
        // (returnType) -> returnType
        errorSetMH = MethodHandles.foldArguments(MethodHandles.identity(returnType), errorSetMH);
        return MethodHandles.filterReturnValue(downcallMH, errorSetMH);
    }

    public static MethodHandle fillErrorAfterReturn(MethodHandle downcallMH, ErrorNo.ErrorNoType[] errorNoType) {
        Class<?> returnType = downcallMH.type().returnType();
        MethodHandle fillTLErrorByTypesMH = FILL_ERROR_CODE_BY_TYPES_MH.bindTo(errorNoType);
        if (returnType == void.class) {
            return MethodHandles.filterReturnValue(downcallMH, fillTLErrorByTypesMH);
        }

        // (returnType) -> void
        MethodHandle errorSetMH = MethodHandles.dropArguments(fillTLErrorByTypesMH, 0, returnType);
        // (returnType) -> returnType
        errorSetMH = MethodHandles.foldArguments(MethodHandles.identity(returnType), errorSetMH);
        return MethodHandles.filterReturnValue(downcallMH, errorSetMH);
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

    private static void fillTLErrorAuto() {
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

    private static void fillTLErrorByTypes(ErrorNo.ErrorNoType[] errorNoTypes) {
        MemorySegment errorNoBuffer = errorBuffer.get();
        boolean isWin = GetLastError_HANDLE != null && WSAGetLastError_HANDLE != null;
        int errorNo = 0, getLast = 0, wSAGetLast = 0;
        for (ErrorNo.ErrorNoType errorNoType : errorNoTypes) {
            switch (errorNoType) {
                case POSIX_ERROR_NO -> errorNo = (int) ERROR_HANDLE.get(errorNoBuffer);
                case WINDOWS_GET_LAST_ERROR -> getLast = isWin ? (int) GetLastError_HANDLE.get(errorNoBuffer) : 0;
                case WINDOWS_WSA_GET_LAST_ERROR ->
                        wSAGetLast = isWin ? (int) WSAGetLastError_HANDLE.get(errorNoBuffer) : 0;
            }
        }
        ErrorNo.CapturedErrorState capturedErrorState =
                isWin
                        ? new ErrorNo.WindowsCapturedError(errorNo, getLast, wSAGetLast)
                        : new ErrorNo.PosixCapturedError(errorNo);
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

    static Linker.Option guessErrorNoOption(boolean auto, ErrorNo.ErrorNoType[] errorNoTypes) {
        String[] currentPlatformSupport = Linker.Option.captureStateLayout().memberLayouts().stream()
                .map(MemoryLayout::name)
                .flatMap(Optional::stream)
                .toArray(String[]::new);
        if (auto) {
            return Linker.Option.captureCallState(currentPlatformSupport);
        }
        ArrayList<String> capturedStateList = new ArrayList<>();
        List<String> currentPlatformSupportList = Arrays.asList(currentPlatformSupport);
        for (ErrorNo.ErrorNoType errorNoType : errorNoTypes) {
            String fieldName = errorNoType.fieldName;
            if (currentPlatformSupportList.contains(fieldName)) {
                capturedStateList.add(fieldName);
            } else {
                throw new IllegalArgumentException("current platform dont support " + errorNoType + " as errorno type");
            }
        }
        return Linker.Option.captureCallState(capturedStateList.toArray(String[]::new));
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
}
