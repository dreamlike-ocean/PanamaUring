package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.proxy.ErrorNo;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.util.ArrayList;

public record DowncallContext(FunctionDescriptor fd, Linker.Option[] ops,
                              String functionName, boolean returnPointer,
                              CaptureContext captureContext, ArrayList<Integer> rawMemoryIndex,
                              boolean containFp
) {


    public record CaptureContext(boolean needCaptureStatue, boolean auto, ErrorNo.ErrorNoType[] errorNoTypes) {
    }


}
