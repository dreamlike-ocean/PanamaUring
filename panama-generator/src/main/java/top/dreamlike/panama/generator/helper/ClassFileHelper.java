package top.dreamlike.panama.generator.helper;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;

public class ClassFileHelper {
    public static <T> String toSignature(Class<T> c) {
        return c.describeConstable().get().descriptorString();
    }

    public static <T> ClassDesc toDesc(Class<T> c) {
        return c.describeConstable().get();
    }

    public static ClassDesc toDesc(String className) {
        return ClassDesc.ofDescriptor("L" + className.replace(".", "/") + ";");
    }

    public static MethodTypeDesc toMethodDescriptor(Executable method) {
        return switch (method) {
            case Method m ->
                    MethodType.methodType(m.getReturnType(), method.getParameterTypes()).describeConstable().get();
            case Constructor _ ->
                    MethodType.methodType(void.class, method.getParameterTypes()).describeConstable().get();
        };
    }

    public static String toSignature(Executable method) {
        return toMethodDescriptor(method).descriptorString();
    }

    public static void invokeMethodHandleExactWithAllArgs(Method method, CodeBuilder it) {
        loadAllArgs(method, it);
        it.invokevirtual(
                ConstantDescs.CD_MethodHandle,
                "invokeExact",
                toMethodDescriptor(method)
        );
        it.return_(TypeKind.from(method.getReturnType()));
    }

    public static void loadAllArgs(Method method, CodeBuilder it) {
        Parameter[] parameters = method.getParameters();
        int nextSlot = 1;
        for (int i = 0; i < parameters.length; i++) {
            TypeKind typeKind = TypeKind.from(parameters[i].getType());
            it.loadLocal(typeKind, nextSlot);
            nextSlot += typeKind.slotSize();
        }
    }

    public static void getField(CodeBuilder cb, Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            cb.getstatic(
                    field.getDeclaringClass().describeConstable().get(),
                    field.getName(),
                    field.getType().describeConstable().get()
            );
        } else {
            cb.getfield(
                    field.getDeclaringClass().describeConstable().get(),
                    field.getName(),
                    field.getType().describeConstable().get()
            );
        }
    }

    public static void setStatic(CodeBuilder cb, Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            cb.putstatic(
                    field.getDeclaringClass().describeConstable().get(),
                    field.getName(),
                    field.getType().describeConstable().get()
            );
        } else {
            cb.putfield(
                    field.getDeclaringClass().describeConstable().get(),
                    field.getName(),
                    field.getType().describeConstable().get()
            );
        }
    }

    public static void invoke(CodeBuilder cb, Executable method) {
        invoke(cb, method, false);
    }

    public static void invoke(CodeBuilder cb, Executable method, boolean interfaceMethod) {

        if (Modifier.isStatic(method.getModifiers())) {
            cb.invokestatic(
                    toDesc(method.getDeclaringClass()),
                    method.getName(),
                    toMethodDescriptor(method)
            );
            return;
        }
        if (interfaceMethod && method instanceof Method) {
            cb.invokeinterface(
                    toDesc(method.getDeclaringClass()),
                    method.getName(),
                    toMethodDescriptor(method)
            );
            return;
        }

        if (method instanceof Constructor<?> || Modifier.isPrivate(method.getModifiers())) {
            cb.invokespecial(
                    toDesc(method.getDeclaringClass()),
                    method.getName(),
                    toMethodDescriptor(method)
            );
            return;
        }
        cb.invokevirtual(
                toDesc(method.getDeclaringClass()),
                method.getName(),
                toMethodDescriptor(method)
        );
    }

    public static void returnValue(CodeBuilder cb, Class returnType) {
        var type = TypeKind.from(returnType);
        cb.return_(type);
    }


}
