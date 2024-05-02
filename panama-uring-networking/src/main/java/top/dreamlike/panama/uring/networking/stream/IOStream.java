package top.dreamlike.panama.uring.networking.stream;

public sealed class IOStream permits SocketStream, FileStream {
    protected boolean autoRead = true;


}
