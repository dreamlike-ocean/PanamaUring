package top.dreamlike.epoll.extension;

import top.dreamlike.nativeLib.epoll.epoll_h;

import java.util.function.IntConsumer;

/**
 * epoll 注册的监听上下文，包含fd，感兴趣事件，处理器等
 */
public record ListenContext(int fd, int eventMask, IntConsumer readCallback, IntConsumer writeCallback) {

    public ListenContext removeReadCallback() {
        return new ListenContext(fd, eventMask & ~epoll_h.EPOLLIN(), ListenContextFactory.EMPTY.readCallback(), writeCallback);
    }

    public ListenContext removeWriteCallback() {
        return new ListenContext(fd, eventMask & ~epoll_h.EPOLLOUT(), readCallback, ListenContextFactory.EMPTY.writeCallback());
    }

}

