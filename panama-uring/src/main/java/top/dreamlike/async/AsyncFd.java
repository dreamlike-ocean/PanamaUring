package top.dreamlike.async;

import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.epoll.async.EpollAsyncServerSocket;
import top.dreamlike.eventloop.IOUringEventLoop;

sealed public abstract class AsyncFd implements EventLoopAccess permits PlainAsyncFd, AsyncWatchService, AsyncServerSocket, AsyncSocket, EpollAsyncServerSocket {

    protected IOUringEventLoop eventLoop;

    protected AsyncFd(IOUringEventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }
}
