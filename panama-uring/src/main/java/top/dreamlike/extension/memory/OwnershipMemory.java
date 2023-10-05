
package top.dreamlike.extension.memory;

import java.lang.foreign.MemorySegment;

public interface OwnershipMemory {
    MemorySegment resource();
    
    void drop();
}