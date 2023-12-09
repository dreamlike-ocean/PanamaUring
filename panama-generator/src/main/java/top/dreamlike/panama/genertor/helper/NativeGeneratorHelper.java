package top.dreamlike.panama.genertor.helper;

import top.dreamlike.panama.genertor.proxy.NativeCallGenerator;
import top.dreamlike.panama.genertor.proxy.StructProxyGenerator;

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

    public final static MethodHandle REINTERPRET_MH;

    public final static Method REBIND_ASSERT_METHOD;

    public final static Method FETCH_CURRENT_NATIVE_CALL_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_CONTEXT_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR;
    public final static Method FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR;
    public static Supplier<NativeCallGenerator> fetchCurrentNativeCallGenerator;
    public static Supplier<StructProxyContext> fetchCurrentNativeStructGenerator;

    public static final Method LOAD_SO;

    static {
        try {
            FETCH_CURRENT_NATIVE_CALL_GENERATOR = NativeGeneratorHelper.class.getMethod("currentNativeCallGenerator");
            REBIND_ASSERT_METHOD = NativeGeneratorHelper.class.getMethod("assertRebindMemory", MemorySegment.class, MemorySegment.class);
            REINTERPRET_MH = MethodHandles.lookup().findStatic(NativeGeneratorHelper.class, "deReferencePointer", MethodType.methodType(MemorySegment.class, MemorySegment.class, long.class));
            FETCH_CURRENT_STRUCT_CONTEXT_GENERATOR = NativeGeneratorHelper.class.getMethod("currentStructContext");
            FETCH_CURRENT_STRUCT_LAYOUT_GENERATOR = NativeGeneratorHelper.class.getMethod("currentLayout");
            FETCH_CURRENT_STRUCT_GENERATOR_GENERATOR = NativeGeneratorHelper.class.getMethod("currentStructGenerator");
            LOAD_SO = NativeCallGenerator.class.getMethod("loadSo", Class.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static NativeCallGenerator currentNativeCallGenerator() {
        return fetchCurrentNativeCallGenerator.get();
    }

    public static void assertRebindMemory(MemorySegment segment, MemorySegment origin) {
        if (segment.byteSize() != origin.byteSize()) {
            throw new IllegalArgumentException(STR."memorySegment size rebind should equal origin memorySegment size");
        }
    }

    public static MemorySegment deReferencePointer(MemorySegment pointer, long sizeof) {
        return pointer.get(ValueLayout.ADDRESS, 0).reinterpret(sizeof);
    }

    public static StructProxyContext currentStructContext() {
        return fetchCurrentNativeStructGenerator.get();
    }

    public static MemoryLayout currentLayout() {
        return fetchCurrentNativeStructGenerator.get().memoryLayout();
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

        System.out.println(STR. "支持对齐的序列为\{ layouts }, sizeof(layouts): \{ size }, align: \{ align }" );
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
}
