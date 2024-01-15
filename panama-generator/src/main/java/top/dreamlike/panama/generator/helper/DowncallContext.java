package top.dreamlike.panama.generator.helper;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.util.ArrayList;

public record DowncallContext(FunctionDescriptor fd, Linker.Option[] ops,
                              String functionName, boolean returnPointer,
                              boolean needCaptureStatue, ArrayList<Integer> rawMemoryIndex) {
}
