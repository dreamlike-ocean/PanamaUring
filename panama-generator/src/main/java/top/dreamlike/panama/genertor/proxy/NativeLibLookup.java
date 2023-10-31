package top.dreamlike.panama.genertor.proxy;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
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
}
