package top.dreamlike.access;

import top.dreamlike.async.uring.IOUringEventLoop;

public interface EventLoopAccess {

    IOUringEventLoop fetchEventLoop();

}
