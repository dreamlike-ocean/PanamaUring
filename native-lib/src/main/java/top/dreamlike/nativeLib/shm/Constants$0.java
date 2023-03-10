package top.dreamlike.nativeLib.shm;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class Constants$0 {
    static final ValueLayout.OfInt C_INT$LAYOUT = JAVA_INT.withBitAlignment(32);
    static final ValueLayout.OfAddress C_POINTER$LAYOUT = ADDRESS.withBitAlignment(64);

    private final static Linker LINKER = Linker.nativeLinker();

    private final static SymbolLookup SYMBOL_LOOKUP = name -> SymbolLookup.loaderLookup().lookup(name)
            .or(() -> LINKER.defaultLookup().lookup(name));
    private static final FunctionDescriptor shm_open$FUNC = FunctionDescriptor.of(C_INT$LAYOUT,
            C_POINTER$LAYOUT,
            C_INT$LAYOUT,
            C_INT$LAYOUT
    );

    private static final FunctionDescriptor shm_unlink$FUNC = FunctionDescriptor.of(C_INT$LAYOUT,
            C_POINTER$LAYOUT
    );


    final static MethodHandle shm_open$MH = downcallHandle("shm_open", shm_open$FUNC);

    final static MethodHandle shm_unlink$MH = downcallHandle("shm_unlink", shm_unlink$FUNC);


    static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return SYMBOL_LOOKUP.lookup(name).
                map(addr -> LINKER.downcallHandle(addr, fdesc)).
                orElse(null);
    }

}
