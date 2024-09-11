package top.dreamlike.panama.uring.nativelib.libs;

import java.lang.foreign.MemorySegment;

public interface LibMman {

    MemorySegment mmap(MemorySegment addr, long length, int prot, int flags, int fd, long offset);

    int munmap(MemorySegment addr, long length);

    int mprotect(MemorySegment addr, long length, int prot);

    int msync(MemorySegment addr, long length, int flags);

    int madvise(MemorySegment addr, long length, int advice);

    interface Prot {
        int PROT_NONE = 0;
        int PROT_READ = 1;
        int PROT_WRITE = 2;
        int PROT_EXEC = 4;
    }

    interface Flag {
        int MAP_SHARED = 0x01;
        int MAP_PRIVATE = 0x02;
        int MAP_FIXED = 0x10;
        int MAP_ANONYMOUS = 0x20;
        int MAP_ANON = MAP_ANONYMOUS;
        int MAP_GROWSDOWN = 0x00100;
        int MAP_DENYWRITE = 0x00800;
        int MAP_EXECUTABLE = 0x01000;
        int MAP_LOCKED = 0x02000;
        int MAP_NORESERVE = 0x04000;
        int MAP_POPULATE = 0x08000;
        int MAP_NONBLOCK = 0x10000;
        int MAP_STACK = 0x20000;
        int MAP_HUGETLB = 0x40000;
        int MAP_SYNC = 0x80000;
        int MAP_FIXED_NOREPLACE = 0x100000;
    }

    interface Advice {
        int MADV_NORMAL = 0;
        int MADV_RANDOM = 1;
        int MADV_SEQUENTIAL = 2;
        int MADV_WILLNEED = 3;
        int MADV_DONTNEED = 4;
        int MADV_FREE = 8;
        int MADV_REMOVE = 9;
        int MADV_DONTFORK = 10;
        int MADV_DOFORK = 11;
        int MADV_MERGEABLE = 12;
        int MADV_UNMERGEABLE = 13;
        int MADV_HUGEPAGE = 14;
        int MADV_NOHUGEPAGE = 15;
        int MADV_DONTDUMP = 16;
        int MADV_DODUMP = 17;
        int MADV_WIPEONFORK = 18;
        int MADV_KEEPONFORK = 19;
        int MADV_COLD = 20;
        int MADV_PAGEOUT = 21;
        int MADV_POPULATE_READ = 22;
        int MADV_POPULATE_WRITE = 23;
        int MADV_DONTNEED_LOCKED = 24;
        int MADV_COLLAPSE = 25;
        int MADV_HWPOISON = 100;
    }
}
