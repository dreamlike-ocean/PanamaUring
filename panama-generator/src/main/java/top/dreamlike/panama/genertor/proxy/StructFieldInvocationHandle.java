package top.dreamlike.panama.genertor.proxy;

import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

record StructFieldInvocationHandle<T>(StructProxyGenerator generator, Class<T> target,
                                      long offset) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MemorySegment realMemory = ((NativeStructEnhanceMark) proxy).realMemory();
        return generator.enhance(target, realMemory.asSlice(offset));
    }
}
