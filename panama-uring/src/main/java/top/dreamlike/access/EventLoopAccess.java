package top.dreamlike.access;

import top.dreamlike.eventloop.IOUringEventLoop;

public interface EventLoopAccess {

    IOUringEventLoop fetchEventLoop();

}
