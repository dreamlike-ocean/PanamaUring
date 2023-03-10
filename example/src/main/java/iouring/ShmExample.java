package iouring;

import top.dreamlike.fileSystem.WatchService;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.nativeLib.fcntl.fcntl_h;
import top.dreamlike.nativeLib.inotify.inotify_h;
import top.dreamlike.nativeLib.mman.mman_h;
import top.dreamlike.nativeLib.shm.shm;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.util.Scanner;
import java.util.UUID;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public class ShmExample {
    public static void main(String[] args) {
//        /proc/7391/fd

        try (MemorySession session = MemorySession.openConfined()) {
            //整点共享内存
            MemorySegment name = session.allocateUtf8String("testDemo");
            int shm_fd = shm.shm_open(name, fcntl_h.O_RDWR() | fcntl_h.O_CREAT(), 0777);
            System.out.println(shm_fd);

            //找到这个共享内存fd的路径
            MemorySegment pathBuff = session.allocate(64);
            int pid = unistd_h.getpid();
            String fdsPath = String.format("/proc/%d/fd/", pid);
            long readLength = unistd_h.readlink(session.allocateUtf8String(fdsPath + shm_fd), pathBuff, pathBuff.byteSize());
            MemorySegment pathName = pathBuff.asSlice(0, readLength);
            System.out.println(NativeHelper.toString(pathName));

            //扩容共享内存
            int ftruncate = unistd_h.ftruncate(shm_fd, 1024);
            System.out.println("ftruncate:" + ftruncate);

            //mmap映射进来 方便操作
            MemoryAddress memoryAddress = mman_h.mmap(MemoryAddress.NULL, 1024, mman_h.PROT_WRITE() | mman_h.PROT_READ(), mman_h.MAP_SHARED(), shm_fd, 0);
            System.out.println("mmap address:" + memoryAddress);
            //前8个字节处理各种写入数据
            MemorySegment base = MemorySegment.ofAddress(memoryAddress, 1024, MemorySession.global());
            MemorySegment shareMemory = base.asSlice(8);

            //写入共享内存
            String str = UUID.randomUUID().toString();
            shareMemory.setUtf8String(0, str);
            System.out.println("write:" + str);

            WatchService service = new WatchService();
            //监听fd写入事件 看看是不是别的进程读取完通知我们读完了
            int wid = service.register(pathName, inotify_h.IN_MODIFY());
            System.out.println("register wid:" + wid);

//            new Thread(() -> {
//                //模拟另外一个进程来写入这个fd 看一下是不是真的能触发inotify
//                try {
//                    Thread.sleep(10000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                unistd_h.write(shm_fd, MemorySession.global().allocate(JAVA_INT, 1254), 4);
//                System.out.println("after write offset:"+unistd_h.lseek(shm_fd, 0, unistd_h.SEEK_CUR()));
//            }).start();

            //本进程等待事件触发
//            System.out.println(service.selectEvent());

            System.out.println(base.get(JAVA_INT, 0));
            shm.shm_unlike(name);


            new Scanner(System.in).next();
        }
    }
}
