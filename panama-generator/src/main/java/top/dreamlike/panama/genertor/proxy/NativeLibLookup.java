package top.dreamlike.panama.genertor.proxy;

import top.dreamlike.panama.genertor.helper.NativeStructEnhanceMark;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

class NativeLibLookup implements SymbolLookup {

    @Override
    public Optional<MemorySegment> find(String name) {
        return SymbolLookup.loaderLookup()
                .find(name)
                .or(() -> Linker.nativeLinker().defaultLookup().find(name));
    }

    public MethodHandle downcallHandle(String name, FunctionDescriptor functionDescriptor, Linker.Option... options) {
        return find(name)
                .map(functionAddr -> Linker.nativeLinker().downcallHandle(functionAddr, functionDescriptor, options))
                .orElseThrow(() -> new IllegalArgumentException(STR. "cant link \{ name }" ));
    }

    public static MemoryLayout primitiveMapToMemoryLayout(Class source) {
        return switch (source) {
            case Class c when c == int.class -> ValueLayout.JAVA_INT;
            case Class c when c == long.class -> ValueLayout.JAVA_LONG;
            case Class c when c == double.class -> ValueLayout.JAVA_DOUBLE;
            case Class c when c == float.class -> ValueLayout.JAVA_FLOAT;
            case Class c when c == byte.class -> ValueLayout.JAVA_BYTE;
            case Class c when c == boolean.class -> ValueLayout.JAVA_BOOLEAN;
            case Class c when c == char.class -> ValueLayout.JAVA_CHAR;
            case Class c when c == short.class -> ValueLayout.JAVA_SHORT;
            case Class c when NativeStructEnhanceMark.class.isAssignableFrom(c) -> ValueLayout.ADDRESS;
            default -> null;
        };
    }
}
