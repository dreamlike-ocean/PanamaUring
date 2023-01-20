package top.dreamlike.async;

import top.dreamlike.access.EventLoopAccess;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;

sealed public interface AsyncFd extends EventLoopAccess permits AsyncFile, AsyncSocket, AsyncServerSocket {
}
