package top.dreamlike.panama.genertor.helper;

import top.dreamlike.panama.genertor.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.foreign.MemoryLayout.paddingLayout;

public class NativeHelper {

    private final static MethodHandle REAL_MEMORY_MH;

    public final static Method EMPTY_METHOD;

    public final static Method MH_CALL_METHOD;

    static {
        try {
            REAL_MEMORY_MH = MethodHandles.lookup().findVirtual(NativeStructEnhanceMark.class, "realMemory", MethodType.methodType(MemorySegment.class));
            EMPTY_METHOD = NativeHelper.class.getMethod("empty");
            MH_CALL_METHOD = MethodHandle.class.getMethod("invokeWithArguments", Object[].class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * @param methodHandle 原始的native methodHandle
     * @param pos          struct转point的参数位置
     * @return 修正过的mh 支持直接struct转point
     */
    public static MethodHandle adjustStructToPoint(MethodHandle methodHandle, int pos) {
        return MethodHandles.filterArguments(
                methodHandle,
                pos,
                REAL_MEMORY_MH
        );
    }

    public static void empty() {
        System.out.println("Empty");
    }

    public static Function<MemorySegment, Object> memoryBinder(MethodHandle methodHandle, MemoryLayout memoryLayout, StructProxyGenerator generator) throws Throwable {
        CallSite callSite = LambdaMetafactory.metafactory(
                MethodHandles.lookup(),
                "apply",
                MethodType.methodType(Function.class, MemoryLayout.class, StructProxyGenerator.class),
                MethodType.methodType(Object.class, Object.class),
                methodHandle,
                MethodType.methodType(Object.class, MemorySegment.class)
        );

        MethodHandle target = callSite.getTarget();
        Function<MemorySegment, Object> lambdaFunction = (Function<MemorySegment, Object>) target.invoke(memoryLayout, generator);
        return (ms) -> {
            return lambdaFunction.apply(ms.reinterpret(memoryLayout.byteSize()));
        };
    }

    public static Supplier<Object> ctorBinder(MethodHandle methodHandle, Class enhanceClass) throws Throwable {

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
