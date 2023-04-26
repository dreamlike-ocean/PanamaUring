package top.dreamlike.epoll.extension;

import java.util.function.IntConsumer;

/**
 * epoll 注册的监听上下文，包含fd，感兴趣事件，处理器等
 */
public record ListenContext(int fd, int eventMask, IntConsumer callback) {
}
