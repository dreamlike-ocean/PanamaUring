package top.dreamlike.panama.generator.helper;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.util.ArrayList;
import java.util.Arrays;

public record DowncallContext(FunctionDescriptor fd, Linker.Option[] ops,
                              String functionName, boolean returnPointer,
                              boolean needCaptureStatue, ArrayList<Integer> rawMemoryIndex,
                              boolean containFp
                              ) {

    public boolean fast() {
        return Arrays
                .stream(ops)
                .anyMatch(op -> op == Linker.Option.critical(true) || op == Linker.Option.critical(false));
    }
}
