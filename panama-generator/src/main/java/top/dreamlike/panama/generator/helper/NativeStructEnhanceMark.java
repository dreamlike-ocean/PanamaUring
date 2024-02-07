package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public interface NativeStructEnhanceMark extends NativeAddressable {

    public StructProxyGenerator fetchStructProxyGenerator();

    public long sizeof();

    public MemoryLayout layout();

    public void rebind(MemorySegment memorySegment);
}
