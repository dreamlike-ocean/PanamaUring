package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;

import java.net.SocketAddress;

public class AsyncTcpSocket implements IoUringAsyncFd {
    private final IoUringEventLoop ioUringEventLoop;

    private final int fd;

    private SocketAddress localAddress;

    private SocketAddress remoteAddress;

    public AsyncTcpSocket(IoUringEventLoop ioUringEventLoop, int fd, SocketAddress localAddress, SocketAddress remoteAddress) {
        this.ioUringEventLoop = ioUringEventLoop;
        this.fd = fd;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public IoUringEventLoop owner() {
        return ioUringEventLoop;
    }

    @Override
    public int fd() {
        return fd;
    }

    @Override
    public String toString() {
        return STR."AsyncTcpSocket{ioUringEventLoop=\{ioUringEventLoop}, fd=\{fd}, localAddress=\{localAddress}, remoteAddress=\{remoteAddress}\{'}'}";
    }
}
