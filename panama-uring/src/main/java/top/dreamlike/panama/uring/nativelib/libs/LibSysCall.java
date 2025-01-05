package top.dreamlike.panama.uring.nativelib.libs;

import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;

public interface LibSysCall {
    MemorySegment SYSCALL_FP = Linker.nativeLinker()
            .defaultLookup()
            .findOrThrow("syscall");

    long io_uring_setup_sys_number = 425L;
    long io_uring_enter_sys_number = 426L;
    long io_uring_register_sys_number = 427L;
}
