package top.dreamlike.panama.generator.helper;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ClassFileHelper {
    public static String toSignature(Class c) {
        if(c.isArray()) {
            return "[" + toSignature(c.getComponentType());
        }
        TypeKind typeKind = calType(c);
        return c.isPrimitive() ? typeKind.descriptor() : "L"+ c.getName().replace(".", "/") + ";";
    }

    public static ClassDesc toDesc(Class c) {
        return ClassDesc.ofDescriptor(toSignature(c));
    }

    public static ClassDesc toDesc(String className) {
        return ClassDesc.ofDescriptor("L" + className.replace(".", "/") + ";");
    }

    public static String toSignature(Executable method) {
        String paramSignature = Arrays.stream(method.getParameters())
                .map(Parameter::getType)
                .map(ClassFileHelper::toSignature)
                .collect(Collectors.joining(""));
        String returnType = switch (method) {
            case Method m -> toSignature(m.getReturnType());
            case Constructor _ -> toSignature(void.class);
        };
        return "(" + paramSignature + ")" + returnType;
    }
    
    public static void invokeMethodHandleExactWithAllArgs(Method method, CodeBuilder it) {
        loadAllArgs(method, it);
        it.invokevirtual(
                toDesc(MethodHandle.class),
                "invokeExact",
               toMethodDescriptor(method)
        );
        it.returnInstruction(calType(method.getReturnType()));
    }

    public static void loadAllArgs(Method method, CodeBuilder it) {
        Parameter[] parameters = method.getParameters();
        int nextSlot = 1;
        for (int i = 0; i < parameters.length; i++) {
            TypeKind typeKind = calType(parameters[i].getType());
            it.loadInstruction(typeKind, nextSlot);
            nextSlot += typeKind.slotSize();
        }
    }
    
    public static MethodTypeDesc toMethodDescriptor(Executable method) {
        return MethodTypeDesc.ofDescriptor(toSignature(method));
    }

    public static void getStaticField(CodeBuilder cb, Field field) {
        cb.getstatic(
                ClassDesc.ofDescriptor(toSignature(field.getDeclaringClass())),
                field.getName(),
                ClassDesc.ofDescriptor(toSignature(field.getType()))
        );
    }

    public static void setStaticField(CodeBuilder cb, Field field) {
        cb.putstatic(
                ClassDesc.ofDescriptor(toSignature(field.getDeclaringClass())),
                field.getName(),
                ClassDesc.ofDescriptor(toSignature(field.getType()))
        );
    }

    public static void getField(CodeBuilder cb, Field field) {
        cb.getfield(
                ClassDesc.ofDescriptor(toSignature(field.getDeclaringClass())),
                field.getName(),
                ClassDesc.ofDescriptor(toSignature(field.getType()))
        );
    }

    public static void setStatic(CodeBuilder cb, Field field) {
        cb.putfield(
                ClassDesc.ofDescriptor(toSignature(field.getDeclaringClass())),
                field.getName(),
                ClassDesc.ofDescriptor(toSignature(field.getType()))
        );
    }

    public static void invoke(CodeBuilder cb, Executable method) {
        invoke(cb, method, false);
    }

    public static void invoke(CodeBuilder cb, Executable method, boolean interfaceMethod) {

        if (Modifier.isStatic(method.getModifiers())) {
            cb.invokestatic(
                    ClassDesc.ofDescriptor(toSignature(method.getDeclaringClass())),
                    method.getName(),
                    MethodTypeDesc.ofDescriptor(toSignature(method))
            );
            return;
        }
        if (interfaceMethod && method instanceof Method) {
            cb.invokeinterface(
                    ClassDesc.ofDescriptor(toSignature(method.getDeclaringClass())),
                    method.getName(),
                    MethodTypeDesc.ofDescriptor(toSignature(method))
            );
            return;
        }

        if (method instanceof Constructor<?> || Modifier.isPrivate(method.getModifiers())) {
            cb.invokespecial(
                    ClassDesc.ofDescriptor(toSignature(method.getDeclaringClass())),
                    method.getName(),
                    MethodTypeDesc.ofDescriptor(toSignature(method))
            );
            return;
        }
        cb.invokevirtual(
                ClassDesc.ofDescriptor(toSignature(method.getDeclaringClass())),
                method.getName(),
                MethodTypeDesc.ofDescriptor(toSignature(method))
        );
    }

    public static void returnValue(CodeBuilder cb, Class returnType) {
        var type = calType(returnType);
        cb.returnInstruction(type);
    }

    public static TypeKind calType(Class type) {
        return switch (type.getName()) {
            case "int" -> TypeKind.IntType;
            case "long" -> TypeKind.LongType;
            case "float" -> TypeKind.FloatType;
            case "double" -> TypeKind.DoubleType;
            case "boolean" -> TypeKind.BooleanType;
            case "byte" -> TypeKind.ByteType;
            case "char" -> TypeKind.CharType;
            case "short" -> TypeKind.ShortType;
            case "void" -> TypeKind.VoidType;
            default -> TypeKind.ReferenceType;
        };
    }


}
