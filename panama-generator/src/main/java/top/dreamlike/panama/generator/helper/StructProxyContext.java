package top.dreamlike.panama.generator.helper;

import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.MemoryLayout;

public record StructProxyContext(StructProxyGenerator generator, MemoryLayout memoryLayout) {
}
