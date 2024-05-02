package top.dreamlike.panama.uring.networking.stream;

import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpSocketFd;

public final class SocketStream extends IOStream {

    private final AsyncMultiShotTcpSocketFd socketFd;


    public SocketStream(AsyncMultiShotTcpSocketFd socketFd) {
        this.socketFd = socketFd;
    }


}
