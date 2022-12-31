package top.dreamlike.helper;

import top.dreamlike.nativeLib.errno.errno_h;
import top.dreamlike.nativeLib.in.in_addr;
import top.dreamlike.nativeLib.in.sockaddr_in;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;

import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.O_NONBLOCK;
import static top.dreamlike.nativeLib.inet.inet_h.*;
import static top.dreamlike.nativeLib.socket.socket_h.listen;
import static top.dreamlike.nativeLib.string.string_h.bzero;

public class SocketHelper {
    public static int serverListen(String host, int port){
        int listenfd  = socket(AF_INET(), SOCK_STREAM(), 0);
        if (listenfd  == -1) throw new IllegalStateException("open listen socket fail");
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment serverAddr = sockaddr_in.allocate(session);
            bzero(serverAddr,sockaddr_in.sizeof());
            sockaddr_in.sin_family$set(serverAddr, (short) AF_INET());
            inet_pton(AF_INET(),session.allocateUtf8String(host),sockaddr_in.sin_addr$slice(serverAddr));
            sockaddr_in.sin_port$set(serverAddr,htons((short) port));
            int bind = bind(listenfd, serverAddr, (int) sockaddr_in.sizeof());
            if (bind == -1) {
                int errorNo = errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
                throw new IllegalStateException("bind error!"+errorNo);
            }
        }
        listen(listenfd, 64);
        return listenfd;
    }

    public static int getErrorNo(){
        return  errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
    }

    public static int makeNoBlocking(int fd){
        int flags = fcntl(fd, F_GETFL(), 0);
        if (flags == -1){
            return errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
        }
        int res = fcntl(fd, F_SETFL(), flags | O_NONBLOCK());
        if (res == -1){
            return errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
        }
        return 0;
    }
}
