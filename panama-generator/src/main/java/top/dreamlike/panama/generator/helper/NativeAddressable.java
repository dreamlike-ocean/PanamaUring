package top.dreamlike.panama.generator.helper;

import java.lang.foreign.MemorySegment;

@FunctionalInterface
public interface NativeAddressable {
    static NativeAddressable of(MemorySegment segment) {
        return () -> segment;
    }

    MemorySegment address();
}
