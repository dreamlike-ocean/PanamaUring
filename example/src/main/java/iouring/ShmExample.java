package iouring;

import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.fcntl.fcntl_h;
import top.dreamlike.nativeLib.mman.mman_h;
import top.dreamlike.nativeLib.shm.shm;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.Scanner;

public class ShmExample {
    public static void main(String[] args) {
//        /proc/7391/fd

        try (MemorySession session = MemorySession.openConfined()) {
            int pid = unistd_h.getpid();
            String fdsPath = String.format("/proc/%d/fd/", pid);
            MemorySegment name = session.allocateUtf8String("testDemo");
            int shm_fd = shm.shm_open(name, fcntl_h.O_RDWR() | fcntl_h.O_CREAT(), 0777);
            System.out.println(shm_fd);
            MemorySegment pathBuff = session.allocate(64);
            long readLength = unistd_h.readlink(session.allocateUtf8String(fdsPath + "/" + shm_fd), pathBuff, pathBuff.byteSize());
            MemorySegment pathName = pathBuff.asSlice(0, readLength);
            System.out.println(NativeHelper.toString(pathName));

            int ftruncate = unistd_h.ftruncate(shm_fd, 1024);
            System.out.println("ftruncate:" + ftruncate);
            MemoryAddress memoryAddress = mman_h.mmap(MemoryAddress.NULL, 1024, mman_h.PROT_WRITE() | mman_h.PROT_READ(), mman_h.MAP_SHARED(), shm_fd, 0);
            System.out.println("mmap address:" + memoryAddress);
            new Scanner(System.in).next();
        }
    }
}
