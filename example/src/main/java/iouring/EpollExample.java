package iouring;

import top.dreamlike.epoll.Epoll;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.unistd.unistd_h.read;

/**
 * 用epoll监听console事件
 */
public class EpollExample {
    public static void main(String[] args) {
        try (Arena allocator = Arena.openConfined()) {
            MemorySegment memorySegment = allocator.allocate(1024);
            int flags = fcntl(0, F_GETFL(), 0);
            int res = fcntl(0, F_SETFL(), flags | O_NONBLOCK());

            Epoll epoll = new Epoll();
            int register = epoll.register(0, EPOLLIN());

            ArrayList<Epoll.Event> events = epoll.select(-1);
            System.out.println(events.get(0).fd());

            long read = read(0, memorySegment, memorySegment.byteSize());
            System.out.println(read);
        }



    }
}
