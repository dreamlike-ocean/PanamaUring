package top.dreamlike.panama.genertor.helper;

import top.dreamlike.panama.genertor.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;

public record StructProxyContext(StructProxyGenerator generator, MemoryLayout memoryLayout) {
}
