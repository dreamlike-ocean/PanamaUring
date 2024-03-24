package top.dreamlike.panama.uring.async.cancel;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

public record IoUringCancelToken(IoUringEventLoop eventLoop, long token) implements CancelToken {
}
