package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.helper.NativeAddressable;
import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

public final class NativeArrayPointer<T> implements NativeStructEnhanceMark, NativeAddressable {
    private MemorySegment pointer;

    private final MemoryLayout elementLayout;

    private final StructProxyGenerator generator;

    private final Class<T> tClass;

    public NativeArrayPointer(StructProxyGenerator generator, MemorySegment memorySegment, Class<T> component) {
        if (component == Object.class) {
            throw new IllegalArgumentException("please fill generic param");
        }
        this.pointer = MemorySegment.ofAddress(memorySegment.address()).reinterpret(Long.MAX_VALUE);
        this.elementLayout = generator.extract(component);
        this.generator = generator;
        this.tClass = component;
    }

    public T getAtIndex(int index) {
        var offset = index * elementLayout.byteSize();
        MemorySegment elementMemory = pointer.asSlice(offset, elementLayout.byteSize());
        return generator.enhance(tClass, elementMemory);
    }

    @Override
    public StructProxyGenerator fetchStructProxyGenerator() {
        return generator;
    }

    @Override
    public MemorySegment realMemory() {
        return pointer;
    }

    @Override
    public MemoryLayout layout() {
        return ValueLayout.ADDRESS;
    }

    @Override
    public void rebind(MemorySegment memorySegment) {
        Objects.requireNonNull(memorySegment);
        this.pointer = MemorySegment.ofAddress(memorySegment.address()).reinterpret(Long.MAX_VALUE);
    }

    @Override
    public MemorySegment address() {
        return pointer;
    }
}
