package top.dreamlike.panama.generator.helper;

import java.lang.foreign.MemorySegment;

public record FunctionPointer(MemorySegment fnAddress) implements NativeAddressable {

    @Override
    public MemorySegment realMemory() {
        return fnAddress;
    }
}
