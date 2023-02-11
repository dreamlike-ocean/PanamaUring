package iouring;

import top.dreamlike.eventloop.EpollEventLoop;
import top.dreamlike.nativeLib.eventfd.EventFd;

import static top.dreamlike.nativeLib.epoll.epoll_h.EPOLLIN;

public class EventFdExample {
    public static void main(String[] args) throws InterruptedException {
        EventFd eventFd = new EventFd();


        EpollEventLoop epollEventLoop = new EpollEventLoop();
        epollEventLoop.start();
        epollEventLoop.registerEvent(eventFd.getFd(), EPOLLIN(), (i) -> {
            System.out.println(eventFd.readSync());
        });

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                eventFd.write(1);
            }
        }).start();
    }
}
