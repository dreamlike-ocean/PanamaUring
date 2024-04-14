package top.dreamlike.panama.uring.nativelib.libs;

import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.proxy.NativeArrayPointer;
import top.dreamlike.panama.uring.nativelib.struct.epoll.NativeEpollEvent;

public interface LibEpoll {

    @NativeFunction(value = "epoll_create1")
    int epoll_create(int flag);

    int epoll_ctl(int epfd, int op, int fd, @Pointer NativeEpollEvent nativeEpollEvent);

    //The timeout argument specifies the number of milliseconds
    int epoll_wait(int epfd, NativeArrayPointer<NativeEpollEvent> events, int maxEvents, int timeout);

    int EPOLL_CLOEXEC = 524288;

    int EPOLL_CTL_ADD = 1;
    int EPOLL_CTL_DEL = 2;
    int EPOLL_CTL_MOD = 3;

    int EPOLLIN = 1;
    int EPOLLPRI = 2;
    int EPOLLOUT = 4;
    int EPOLLRDNORM = 64;

    int EPOLLRDBAND = 128;

    int EPOLLWRNORM = 256;

    int EPOLLWRBAND = 512;

    int EPOLLMSG = 1024;

    int EPOLLERR = 8;

    int EPOLLHUP = 16;

    int EPOLLRDHUP = 8192;

    int EPOLLEXCLUSIVE = 268435456;

    int EPOLLWAKEUP = 536870912;

    int EPOLLONESHOT = 1073741824;

    int EPOLLET = -2147483648;
}
    

