package top.dreamlike.panama.genertor.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

record MethodHandleInvocationHandle(MethodHandle target) implements InvocationHandler {

    @Override
    public Object invoke(Object __, Method ___, Object[] args) throws Throwable {
        return target.invokeWithArguments(args);
    }
}
