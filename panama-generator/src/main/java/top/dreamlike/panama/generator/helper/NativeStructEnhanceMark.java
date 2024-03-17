package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

public interface NativeStructEnhanceMark {

    public StructProxyGenerator fetchStructProxyGenerator();

    MemorySegment realMemory();

    public default long sizeof() {
        return layout().byteSize();
    }

    public MemoryLayout layout();

    public void rebind(MemorySegment memorySegment);
}
