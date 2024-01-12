package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public interface NativeStructEnhanceMark {

    public StructProxyGenerator fetchStructProxyGenerator();

    public MemorySegment realMemory();

    public long sizeof();

    public MemoryLayout layout();

    public void rebind(MemorySegment memorySegment);
}
