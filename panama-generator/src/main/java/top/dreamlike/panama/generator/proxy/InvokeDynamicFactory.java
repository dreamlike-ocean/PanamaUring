package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.marco.Condition;

import java.lang.invoke.*;
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
        MethodHandle nativeCallMH = generator.nativeMethodHandle(method);
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

}
