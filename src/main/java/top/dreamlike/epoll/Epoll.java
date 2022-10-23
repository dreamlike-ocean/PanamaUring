package top.dreamlike.epoll;

import top.dreamlike.nativeLib.epoll.epoll_data;
import top.dreamlike.nativeLib.epoll.epoll_event;
import top.dreamlike.nativeLib.errno.errno_h;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.unistd.unistd_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;

import static top.dreamlike.nativeLib.epoll.epoll_h.*;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.inet.inet_h.htons;
import static top.dreamlike.nativeLib.inet.inet_h.inet_pton;
import static top.dreamlike.nativeLib.socket.socket_h.*;
import static top.dreamlike.nativeLib.string.string_h.bzero;

//除了静态函数 其他都是线程不安全的
public class Epoll implements AutoCloseable{
    int epollFd;

    private final MemorySession allocator;

    private final MemorySegment events;

    public Epoll(){
        allocator = MemorySession.openShared();
        events = allocator.allocateArray(epoll_event.$LAYOUT(), 1024);
        epollFd = epoll_create1(0);
        if (epollFd == -1 ){
            throw new IllegalStateException("epoll create fail");
        }
    }


    public int register(int fd,int event){
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment epollEvent = epoll_event.allocate(session);
            epoll_event.events$set(epollEvent, event);
            epoll_data.fd$set(epoll_event.data$slice(epollEvent), fd);
            return epoll_ctl(epollFd, EPOLL_CTL_ADD(),fd,epollEvent);
        }
    }


    public ArrayList<Event> select(int timeout){
        int count = epoll_wait(epollFd, events, 1024, timeout);
        ArrayList<Event> fds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int fd = epoll_event.fd$get(events, i);
            int event = epoll_event.events$get(events, i);
            fds.add(new Event(fd,event));
        }

        return fds;
    }




    @Override
    public void close() throws Exception {
        unistd_h.close(epollFd);
        allocator.close();
    }

    public static class Event{
        public final int fd;
        public final int event;

        public Event(int fd, int event) {
            this.fd = fd;
            this.event = event;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "fd=" + fd +
                    ", event=" + event +
                    '}';
        }
    }
}
