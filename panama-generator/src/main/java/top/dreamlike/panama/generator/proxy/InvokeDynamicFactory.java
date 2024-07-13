package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.helper.NativeGeneratorHelper;
import top.dreamlike.panama.generator.marco.Condition;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class InvokeDynamicFactory {
    public static CallSite nativeCallIndyFactory(MethodHandles.Lookup lookup, String methodName, MethodType methodType) throws Throwable {
        if (Condition.DEBUG) {
            System.out.println("call nativeCallIndyFactory lookup:" + lookup + " methodName:" + methodName + " methodType:" + methodType);
        }
        //这里的lookup是当前的代理类
        Class<?> lookupClass = lookup.lookupClass();
        NativeCallGenerator generator = ((NativeCallGenerator) lookup.findStaticVarHandle(lookupClass, NativeCallGenerator.GENERATOR_FIELD_NAME, NativeCallGenerator.class).get());
        Class<?> targetInterface = lookupClass.getInterfaces()[0];
        Method method = targetInterface.getMethod(methodName, methodType.parameterArray());
        //本来就是lazy的所以这里直接寻找对应符号地址然后绑定就行了
        MethodHandle nativeCallMH = generator.nativeMethodHandle(method, false);
        return new ConstantCallSite(nativeCallMH);
    }

    public static CallSite shortcutIndyFactory(MethodHandles.Lookup lookup, String methodName, MethodType methodType, Object... args) throws Throwable {
        if (Condition.DEBUG) {
            System.out.println("call shortcutIndyFactory lookup:" + lookup + " methodName:" + methodName + " methodType:" + methodType);
        }
        Class<?> lookupClass = lookup.lookupClass();
        StructProxyGenerator generator = ((StructProxyGenerator) lookup.findStaticVarHandle(lookupClass, StructProxyGenerator.GENERATOR_FIELD, StructProxyGenerator.class).get());
        Class<?> targetInterface = lookupClass.getInterfaces()[0];
        Method method = targetInterface.getMethod(methodName, methodType.parameterArray());
        return new ConstantCallSite(generator.generateShortcutTrustedMH(method));
    }

    /**
     * 默认已经 aload0
     *
     * @param lookup
     * @param fieldName
     * @param methodType
     * @param args
     * @return
     * @throws Throwable
     */
    public static CallSite nativeStructGetterIndyFactory(MethodHandles.Lookup lookup, String fieldName, MethodType methodType, Object... args) throws Throwable {
        Class<?> proxyClass = lookup.lookupClass();
        StructProxyGenerator generator = (StructProxyGenerator) lookup.findStaticVarHandle(proxyClass, StructProxyGenerator.GENERATOR_FIELD, StructProxyGenerator.class).get();
        MemoryLayout currentLayout = (MemoryLayout) lookup.findStaticVarHandle(proxyClass, StructProxyGenerator.LAYOUT_FIELD, MemoryLayout.class).get();
        Field field = proxyClass.getSuperclass().getDeclaredField(fieldName);
        Class<?> fieldType = field.getType();
        MethodHandle methodHandle = switch (fieldType) {
            case Class c when c.isPrimitive() ->
                    nativeStructPrimitiveGetterIndyFactory(lookup, generator, currentLayout, field, fieldName);
            case Class c when NativeArray.class.isAssignableFrom(c) ->
                    nativeStructNativeArrayGetterIndyFactory(lookup, generator, currentLayout, field, fieldName);
            case Class c when MemorySegment.class.isAssignableFrom(c) ->
                    nativeStructMemorySegmentGetterIndyFactory(lookup, generator, currentLayout, field, fieldName);
            default -> nativeStructSubStructGetterIndyFactory(lookup, generator, currentLayout, field, fieldName);
        };

        return new ConstantCallSite(methodHandle);

    }

    private static MethodHandle nativeStructPrimitiveGetterIndyFactory(MethodHandles.Lookup lookup, StructProxyGenerator __, MemoryLayout currentLayout, Field field, String fieldName) {
        VarHandle varHandle = StructProxyGenerator.generateVarHandle(currentLayout, fieldName);
        varHandle = MethodHandles.insertCoordinates(varHandle, 1, 0);
        MethodHandle getter = varHandle.toMethodHandle(VarHandle.AccessMode.GET);
        getter = MethodHandles.filterArguments(getter, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        return getter;
    }

    private static MethodHandle nativeStructNativeArrayGetterIndyFactory(MethodHandles.Lookup lookup, StructProxyGenerator proxyGenerator, MemoryLayout currentLayout, Field field, String fieldName) throws Throwable {
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        if (arrayMark == null) {
            throw new IllegalArgumentException(field + " must be marked as NativeArrayMark");
        }
        long offset = currentLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        long newSize = proxyGenerator.extract(arrayMark.size()).byteSize() * arrayMark.length();
        boolean asPointer = field.getAnnotation(Pointer.class) != null || arrayMark.asPointer();
        MethodHandle nativeArrayCtorMh = NativeGeneratorHelper.NATIVE_ARRAY_CTOR
                .bindTo(proxyGenerator);
        //memorySegment -> NativeArray
        nativeArrayCtorMh = MethodHandles.insertArguments(nativeArrayCtorMh, 1, field.getDeclaringClass());
        //当前只需要memorySegment就行了 需要将this转为对应MemorySegment
        MethodHandle memorySegmentSupplierMH;
        if (asPointer) {
            //resize : memory,int -> memory
            MethodHandle resizeMh = lookup.unreflect(NativeGeneratorHelper.REINTERPRET);
            // memory -> memory
            resizeMh = MethodHandles.insertArguments(resizeMh, 1, newSize);

            //getPtr: memory,addressLayout,offset -> memory
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT);
            handle = MethodHandles.insertArguments(handle, 1, ValueLayout.ADDRESS, offset);

            // resize(getPtr(param1))
            memorySegmentSupplierMH = MethodHandles.filterArguments(resizeMh, 0, handle);

            //resize(getPtr(toStruct(this)))
            // Object -> MemorySegment
            memorySegmentSupplierMH = MethodHandles.filterArguments(memorySegmentSupplierMH, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        } else {
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.AS_SLICE);
            handle = MethodHandles.insertArguments(handle, 1, offset, newSize);
            memorySegmentSupplierMH = MethodHandles.filterArguments(handle, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        }
        nativeArrayCtorMh = MethodHandles.filterArguments(nativeArrayCtorMh, 0, memorySegmentSupplierMH);
        return nativeArrayCtorMh;
    }

    private static MethodHandle nativeStructMemorySegmentGetterIndyFactory(MethodHandles.Lookup lookup, StructProxyGenerator proxyGenerator, MemoryLayout currentLayout, Field field, String fieldName) throws Throwable {
        long offset = currentLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        NativeArrayMark arrayMark = field.getAnnotation(NativeArrayMark.class);
        boolean pointerMarked = field.getAnnotation(Pointer.class) != null;
        boolean pointer = arrayMark.asPointer();
        //当前只需要memorySegment就行了 需要将this转为对应MemorySegment

        if (pointerMarked) {
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT);
            handle = MethodHandles.insertArguments(handle, 1, ValueLayout.ADDRESS, offset);
            return MethodHandles.filterArguments(handle, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        }
        MethodHandle memorySegmentSupplierMH;
        long newSize = proxyGenerator.extract(arrayMark.size()).byteSize() * arrayMark.length();
        if (pointer) {
            //resize : memory,int -> memory
            MethodHandle resizeMh = lookup.unreflect(NativeGeneratorHelper.REINTERPRET);
            // memory -> memory
            resizeMh = MethodHandles.insertArguments(resizeMh, 1, newSize);

            //getPtr: memory,addressLayout,offset -> memory
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT);
            handle = MethodHandles.insertArguments(handle, 1, ValueLayout.ADDRESS, offset);

            // resize(getPtr(param1))
            memorySegmentSupplierMH = MethodHandles.filterArguments(resizeMh, 0, handle);

            //resize(getPtr(toStruct(this)))
            // Object -> MemorySegment
            memorySegmentSupplierMH = MethodHandles.filterArguments(memorySegmentSupplierMH, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        } else {
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.AS_SLICE);
            handle = MethodHandles.insertArguments(handle, 1, offset, newSize);
            memorySegmentSupplierMH = MethodHandles.filterArguments(handle, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        }
        return memorySegmentSupplierMH;
    }

    private static MethodHandle nativeStructSubStructGetterIndyFactory(MethodHandles.Lookup lookup, StructProxyGenerator proxyGenerator, MemoryLayout currentLayout, Field field, String fieldName) throws Throwable {
        long subStructLayoutSize = currentLayout.select(MemoryLayout.PathElement.groupElement(field.getName())).byteSize();
        long offset = currentLayout.byteOffset(MemoryLayout.PathElement.groupElement(field.getName()));
        boolean isPointer = field.getAnnotation(Pointer.class) != null;
        MethodHandle memorySegmentSupplierMH;

        if (isPointer) {
            //resize : memory,int -> memory
            MethodHandle resizeMh = lookup.unreflect(NativeGeneratorHelper.REINTERPRET);
            // memory -> memory
            resizeMh = MethodHandles.insertArguments(resizeMh, 1, subStructLayoutSize);

            //getPtr: memory,addressLayout,offset -> memory
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.GET_ADDRESS_FROM_MEMORY_SEGMENT);
            handle = MethodHandles.insertArguments(handle, 1, ValueLayout.ADDRESS, offset);

            // resize(getPtr(param1))
            memorySegmentSupplierMH = MethodHandles.filterArguments(resizeMh, 0, handle);

            //resize(getPtr(toStruct(this)))
            // Object -> MemorySegment
            memorySegmentSupplierMH = MethodHandles.filterArguments(memorySegmentSupplierMH, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        } else {
            MethodHandle handle = lookup.unreflect(NativeGeneratorHelper.AS_SLICE);
            handle = MethodHandles.insertArguments(handle, 1, offset, subStructLayoutSize);
            memorySegmentSupplierMH = MethodHandles.filterArguments(handle, 0, NativeGeneratorHelper.TRANSFORM_OBJECT_TO_STRUCT_MH);
        }

        // memory -> object
        MethodHandle returnEnhance = StructProxyGenerator.ENHANCE_MH
                .asType(StructProxyGenerator.ENHANCE_MH.type().changeReturnType(field.getType()))
                .bindTo(proxyGenerator)
                .bindTo(field.getType());
        return MethodHandles.filterArguments(returnEnhance, 0, memorySegmentSupplierMH);
    }
}
