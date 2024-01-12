package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

record StructFieldInvocationHandle<T>(StructProxyGenerator generator, Class<T> target,
                                      long offset, boolean isPointer) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MemorySegment fieldMemory = ((NativeStructEnhanceMark) proxy).realMemory();
        //会extract两次 但是第二次会走缓存 问题不大
        MemoryLayout extract = generator.extract(target);
        if (isPointer) {
            MemorySegment pointer = fieldMemory.get(ValueLayout.ADDRESS, offset);
            fieldMemory = pointer.reinterpret(extract.byteSize());
        } else {
            fieldMemory = fieldMemory.asSlice(offset, extract.byteSize());
        }

        return generator.enhance(target, fieldMemory);
    }
}
