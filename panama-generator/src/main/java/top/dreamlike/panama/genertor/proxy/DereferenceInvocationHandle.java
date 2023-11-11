package top.dreamlike.panama.genertor.proxy;

import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.lang.invoke.VarHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

record DereferenceGetterInvocationHandle(VarHandle varHandle) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        NativeStructEnhanceMark mark = (NativeStructEnhanceMark) proxy;
        return varHandle.get(mark.realMemory());
    }

}

record DereferenceSetterInvocationHandle(VarHandle varHandle) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        NativeStructEnhanceMark mark = (NativeStructEnhanceMark) proxy;
        varHandle.set(mark.realMemory(), args[0]);
        return null;
    }

}



