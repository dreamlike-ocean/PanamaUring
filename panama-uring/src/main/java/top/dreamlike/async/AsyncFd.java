package top.dreamlike.async;

import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.file.AsyncWatchService;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;

sealed public interface AsyncFd extends EventLoopAccess permits AsyncFile, AsyncWatchService, AsyncServerSocket, AsyncSocket {
}
