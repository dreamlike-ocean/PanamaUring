package top.dreamlike.extension.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

public class DefaultOwnershipMemory implements OwnershipMemory {
    private final MemorySegment memorySegment;

    private final Consumer<MemorySegment> dropTrait;

    public DefaultOwnershipMemory(MemorySegment memorySegment, Consumer<MemorySegment> releaseFn) {
        if(memorySegment == null) {
            throw new NullPointerException("memorySegment is null");
        }
        if(releaseFn == null) {
            throw new NullPointerException("releaseFn is null");
        }
        this.dropTrait = releaseFn;
        this.memorySegment = memorySegment;
    }

    /**
     *
     * @param memorySegment 所有权对象
     * @param arena memorySegment所属的arena
     */
    public DefaultOwnershipMemory(MemorySegment memorySegment, Arena arena) {
        if(memorySegment == null) {
            throw new NullPointerException("memorySegment is null");
        }
        if(arena == null) {
            throw new NullPointerException("releaseFn is null");
        }
        this.dropTrait = (__) -> {
            arena.close();
        };
        this.memorySegment = memorySegment;
    }


    @Override
    public MemorySegment resource() {
        return memorySegment;
    }

    @Override
    public void drop() {
        dropTrait.accept(memorySegment);
    }
    
}
