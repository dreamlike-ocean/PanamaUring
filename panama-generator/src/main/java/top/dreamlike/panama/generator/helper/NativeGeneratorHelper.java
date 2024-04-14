
package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.exception.StructException;
import top.dreamlike.panama.generator.marco.Condition;
import top.dreamlike.panama.generator.proxy.NativeArray;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.foreign.MemoryLayout.paddingLayout;

public class NativeGeneratorHelper {

    public final static MethodHandle DEREFERENCE;

    public final static Method REBIND_ASSERT_METHOD;

    public final static Method FETCH_CURRENT_NATIVE_CALL_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_CONTEXT_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR;
    public static final ThreadLocal<NativeCallGenerator> CURRENT_GENERATOR = new ThreadLocal<>();
    public static Supplier<NativeCallGenerator> fetchCurrentNativeCallGenerator = CURRENT_GENERATOR::get;

    public static final ThreadLocal<StructProxyContext> STRUCT_CONTEXT = new ThreadLocal<>();
    public static Supplier<StructProxyContext> fetchCurrentNativeStructGenerator = STRUCT_CONTEXT::get;

    public static final Method LOAD_SO;

    public static final Method FETCH_STRUCT_PROXY_GENERATOR;
    public static final Method REAL_MEMORY;

    public static final Method REBIND_MEMORY;

    public static final Method GET_ADDRESS_FROM_MEMORY_SEGMENT;

    public static final Method REINTERPRET;

    public static final Method AS_SLICE;

    public static final Method ENHANCE;

    public static final MethodHandle TRANSFORM_OBJECT_TO_STRUCT_MH;

    public static final MethodHandle NATIVE_ARRAY_CTOR;

    public static final Method SET_PTR;

    public static final Method OVER_WRITE_SUB_ELEMENT;

    static {
        try {
            FETCH_CURRENT_NATIVE_CALL_GENERATOR = NativeGeneratorHelper.class.getMethod("currentNativeCallGenerator");
            REBIND_ASSERT_METHOD = NativeGeneratorHelper.class.getMethod("assertRebindMemory", MemorySegment.class, MemorySegment.class);
            DEREFERENCE = MethodHandles.lookup().findStatic(NativeGeneratorHelper.class, "deReferencePointer", MethodType.methodType(MemorySegment.class, MemorySegment.class, long.class));
            FETCH_CURRENT_STRUCT_CONTEXT_GENERATOR = NativeGeneratorHelper.class.getMethod("currentStructContext");
            FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR = NativeGeneratorHelper.class.getMethod("currentLayout");
            FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR = NativeGeneratorHelper.class.getMethod("currentStructGenerator");
            LOAD_SO = NativeCallGenerator.class.getMethod("loadSo", Class.class);
            FETCH_STRUCT_PROXY_GENERATOR = NativeStructEnhanceMark.class.getMethod("fetchStructProxyGenerator");
            REAL_MEMORY = NativeStructEnhanceMark.class.getMethod("realMemory");
            REBIND_MEMORY = NativeStructEnhanceMark.class.getMethod("rebind", MemorySegment.class);
            GET_ADDRESS_FROM_MEMORY_SEGMENT = MemorySegment.class.getMethod("get", AddressLayout.class, long.class);
            REINTERPRET = MemorySegment.class.getMethod("reinterpret", long.class);
            AS_SLICE = MemorySegment.class.getMethod("asSlice", long.class, long.class);
            ENHANCE = StructProxyGenerator.class.getMethod("enhance", Class.class, MemorySegment.class);
            TRANSFORM_OBJECT_TO_STRUCT_MH = MethodHandles.lookup().findStatic(NativeGeneratorHelper.class, "transToStruct", MethodType.methodType(MemorySegment.class, Object.class));
            NATIVE_ARRAY_CTOR = MethodHandles.lookup().findConstructor(NativeArray.class, MethodType.methodType(void.class, StructProxyGenerator.class, MemorySegment.class, Class.class));
            SET_PTR = NativeGeneratorHelper.class.getMethod("setPtr", Object.class, long.class, Object.class);
            OVER_WRITE_SUB_ELEMENT = NativeGeneratorHelper.class.getMethod("overwriteSubElement", Object.class, long.class, long.class, Object.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static NativeCallGenerator currentNativeCallGenerator() {
        return fetchCurrentNativeCallGenerator.get();
    }

    public static void assertRebindMemory(MemorySegment segment, MemorySegment origin) {
        if (segment.byteSize() != origin.byteSize()) {
            throw new IllegalArgumentException("memorySegment size rebind should equal origin memorySegment size");
        }
    }

    public static MemorySegment deReferencePointer(MemorySegment pointer, long sizeof) {
        if (pointer.address() == MemorySegment.NULL.address()){
            return pointer;
        }
        return pointer.reinterpret(sizeof);
    }

    public static StructProxyContext currentStructContext() {
        return fetchCurrentNativeStructGenerator.get();
    }

    public static MemorySegment transToStruct(Object o) {
        return switch (o) {
            case null -> MemorySegment.NULL;
            case NativeAddressable nativeAddressable -> nativeAddressable.address();
            case NativeStructEnhanceMark structEnhanceMark -> structEnhanceMark.realMemory();
            case MemorySegment memorySegment -> memorySegment;
            default ->
                    throw new StructException(o.getClass() + " is not struct,pleace call StructProxyGenerator::enhance before calling native function");
        };
    }


    public static MemoryLayout currentLayout() {
        StructProxyContext context = fetchCurrentNativeStructGenerator.get();
        if (context == null) {
            new RuntimeException().printStackTrace();
        }
        return context.memoryLayout();
    }

    public static StructProxyGenerator currentStructGenerator() {
        return fetchCurrentNativeStructGenerator.get().generator();
    }

    public static MemoryLayout calAlignLayout(List<MemoryLayout> memoryLayouts) {
        long size = 0;
        long align = 1;
        ArrayList<MemoryLayout> layouts = new ArrayList<>();
        for (MemoryLayout memoryLayout : memoryLayouts) {
            if (size % memoryLayout.byteAlignment() == 0) {
                size = Math.addExact(size, memoryLayout.byteSize());
                align = Math.max(align, memoryLayout.byteAlignment());
                layouts.add(memoryLayout);
                continue;
            }
            long multiple = size / memoryLayout.byteAlignment();
            long padding = (multiple + 1) * memoryLayout.byteAlignment() - size;
            size = Math.addExact(size, padding);
            layouts.add(paddingLayout(padding));
            layouts.add(memoryLayout);
            size = Math.addExact(size, memoryLayout.byteSize());
            align = Math.max(align, memoryLayout.byteAlignment());
        }

        if (size % align != 0) {
            long multiple = size / align;
            long padding = (multiple + 1) * align - size;
            size = Math.addExact(size, padding);
            layouts.add(paddingLayout(padding));
        }
        if (Condition.DEBUG){
            System.out.println("支持对齐的序列为" + layouts + ", sizeof(layouts): " + size + ", align: " + align);
        }
        return MemoryLayout.structLayout(layouts.toArray(MemoryLayout[]::new));
    }

    public static Function<MemorySegment, Object> memoryBinder(MethodHandle methodHandle, MemoryLayout memoryLayout) throws Throwable {
        CallSite callSite = LambdaMetafactory.metafactory(
                MethodHandles.lookup(),
                "apply",
                //返回值为目标sam 其余的为捕获变量
                MethodType.methodType(Function.class),
                //sam的方法签名
                MethodType.methodType(Object.class, Object.class),
                //转发到的目标方法 这里是(MemorySegment.class) -> ${EnhanceClass}
                methodHandle,
                //最终想要的调用形式
                MethodType.methodType(Object.class, MemorySegment.class)
        );

        MethodHandle target = callSite.getTarget();
        //传入捕获的变量
        Function<MemorySegment, Object> lambdaFunction = (Function<MemorySegment, Object>) target.invoke();
        //对传入的memorysegment再reinterpret
        return (ms) -> lambdaFunction.apply(ms.reinterpret(memoryLayout.byteSize()));
    }

    public static Supplier<Object> ctorBinder(MethodHandle methodHandle) throws Throwable {

        CallSite callSite = LambdaMetafactory.metafactory(
                MethodHandles.lookup(),
                "get",
                MethodType.methodType(Supplier.class),
                MethodType.methodType(Object.class),
                methodHandle,
                methodHandle.type()
        );
        return (Supplier<Object>) callSite.getTarget().invoke();
    }

    public static void setPtr(Object proxyStruct, long offset, Object newStructPtr) {
        MemorySegment newPtr = transToStruct(newStructPtr);
        MemorySegment struct = transToStruct(proxyStruct);
        struct.set(ValueLayout.ADDRESS, offset, newPtr);
    }

    public static void overwriteSubElement(Object proxyStruct, long offset, long size, Object newStruct) {
        MemorySegment newStructMemory = transToStruct(newStruct);
        if (newStructMemory.byteSize() != size) {
            throw new StructException("newStruct size must greater than old!");
        }
        MemorySegment struct = transToStruct(proxyStruct);
        MemorySegment.copy(
                newStructMemory, ValueLayout.JAVA_BYTE, 0,
                struct, ValueLayout.JAVA_BYTE, offset,
                size
        );
    }
}

