package top.dreamlike.panama.generator.proxy;

import io.github.dreamlike.unsafe.vthread.TerminatingThreadLocal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
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

    private static final MethodHandle MEMORY_SEGMENT_HEAP_INT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_LONG_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_CHAR_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_FLOAT_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_DOUBLE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_BYTE_MH;
    private static final MethodHandle MEMORY_SEGMENT_HEAP_SHORT_MH;

    public static final MethodHandle CSTR_TOSTRING_MH;

    private static final boolean TERMINATING_THREAD_LOCAL_ENABLE;
    private static final VarHandle errorHandle;

    //sb java 只能枚举全部原始类型了
    static ThreadLocal<MemorySegment> errorBuffer = new ThreadLocal<>();

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            AllocateErrorBuffer_MH = lookup
                    .findStatic(NativeLookup.class, "allocateErrorBuffer", MethodType.methodType(MemorySegment.class));
            errorHandle = MethodHandles.insertCoordinates(Linker.Option.captureStateLayout()
                    .varHandle(MemoryLayout.PathElement.groupElement("errno")), 1, 0);
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

            boolean enableTerminatingThreadLocal;

            if (NativeImageHelper.inExecutable()) {
                enableTerminatingThreadLocal = false;
            } else {
                try {
                    Class.forName("io.github.dreamlike.unsafe.vthread.TerminatingThreadLocal");
                    enableTerminatingThreadLocal = true;
                } catch (Throwable e) {
                    enableTerminatingThreadLocal = false;
                }
            }

            TERMINATING_THREAD_LOCAL_ENABLE = enableTerminatingThreadLocal;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment allocateErrorBuffer() {
        SegmentAllocator allocator = MemoryLifetimeScope.currentAllocator.get();
        StructLayout structLayout = Linker.Option.captureStateLayout();
        MemorySegment buffer;

        if (allocator != null) {
            buffer = allocator.allocate(structLayout);
        } else {
            if (!TERMINATING_THREAD_LOCAL_ENABLE) {
                throw new IllegalStateException("please active MemoryLifetimeScope first!");
            } else {
                //lazy init
                class Holder {
                    static final TerminatingThreadLocal<ErrorBufferHolder> errorBufferHolder = new TerminatingThreadLocal<>() {
                        @Override
                        protected void threadTerminated(ErrorBufferHolder value) {
                            value.allocator.close();
                        }
                    };
                }

                ErrorBufferHolder errorBufferHolder = Holder.errorBufferHolder.get();
                if (errorBufferHolder == null) {
                    Arena arena = Arena.ofConfined();
                    buffer = arena.allocate(structLayout);
                    Holder.errorBufferHolder.set(new ErrorBufferHolder(arena, buffer));
                } else {
                    buffer = errorBufferHolder.buffer;
                }
            }
        }


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

    public MethodHandle downcallHandle(String name, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        return find(name)
                .map(functionAddr -> Linker.nativeLinker().downcallHandle(functionAddr, functionDescriptor, options))
                .orElseThrow(() -> new IllegalArgumentException("cant link " + name));
    }

    record ErrorBufferHolder(Arena allocator, MemorySegment buffer) {
    }


}
