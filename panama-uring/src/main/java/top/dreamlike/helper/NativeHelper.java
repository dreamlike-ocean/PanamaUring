package top.dreamlike.helper;

import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.nativeLib.errno.errno_h;
import top.dreamlike.nativeLib.in.sockaddr_in;
import top.dreamlike.nativeLib.inet.in_addr;
import top.dreamlike.nativeLib.inet.inet_h;
import top.dreamlike.nativeLib.inet.sockaddr_in6;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static top.dreamlike.nativeLib.fcntl.fcntl_h.*;
import static top.dreamlike.nativeLib.inet.inet_h.*;
import static top.dreamlike.nativeLib.socket.socket_h.listen;
import static top.dreamlike.nativeLib.string.string_h.*;

public class NativeHelper {
    private static final String[] errStr;

    //todo ipv6支持 有空会做

    public static int serverListen(String host, int port){
        int socketFd  = socket(AF_INET(), SOCK_STREAM(), 0);
        if (socketFd  == -1) throw new IllegalStateException("open listen socket fail");
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment serverAddr = sockaddr_in.allocate(session);
            bzero(serverAddr,sockaddr_in.sizeof());
            sockaddr_in.sin_family$set(serverAddr, (short) AF_INET());
            inet_pton(AF_INET(),session.allocateUtf8String(host),sockaddr_in.sin_addr$slice(serverAddr));
            sockaddr_in.sin_port$set(serverAddr,htons((short) port));
            int bind = bind(socketFd, serverAddr, (int) sockaddr_in.sizeof());
            if (bind == -1) {
                int errorNo = errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
                throw new IllegalStateException("bind error!"+errorNo);
            }
        }
        listen(socketFd, 64);
        return socketFd;
    }

    public static int tcpClientSocket(){
        int socketFd = inet_h.socket(inet_h.AF_INET(), inet_h.SOCK_STREAM(), 0);
        if (socketFd  == -1) throw new IllegalStateException("open listen socket fail");
        return socketFd;
    }


    public static int tcpClientSocketV6(){
        int socketFd = inet_h.socket(inet_h.AF_INET6(), inet_h.SOCK_STREAM(), 0);
        if (socketFd  == -1) throw new IllegalStateException("open listen socket fail");
        return socketFd;
    }

    /**
     *
     * @param session
     * @param host
     * @param port
     * @return sockAddr to isIpv4
     * @throws UnknownHostException
     */
    public static Pair<MemorySegment,Boolean> getSockAddr(MemorySession session,String host,int port) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(host);
        return switch (inetAddress) {
            case Inet6Address address -> new Pair<>(ipv6(session, address.getHostAddress(),port),false);
            case Inet4Address address -> new Pair<>(ipv4(session,address.getHostAddress(),port),true);
            case InetAddress address -> new Pair<>(ipv4(session,address.getHostAddress(),port),true);
        };
    }

    private static MemorySegment ipv6(MemorySession session,String ipv6,int port){
        MemorySegment sockaddrSegement = sockaddr_in6.allocate(session);
        bzero(sockaddrSegement, sockaddr_in6.sizeof());
        sockaddr_in6.sin6_family$set(sockaddrSegement,(short)inet_h.AF_INET6());
        sockaddr_in6.sin6_port$set(sockaddrSegement,htons((short) port));
        int res = inet_pton(AF_INET6(), session.allocateUtf8String(ipv6), sockaddr_in6.sin6_addr$slice(sockaddrSegement));
        if (res < 0){
            throw new IllegalStateException("inet_pton fail!,err:"+getNowError());
        }
        return sockaddrSegement;
    }

    private static MemorySegment ipv4(MemorySession session,String ipv4,int port){
        MemorySegment sockaddrSegement = sockaddr_in.allocate(session);
        bzero(sockaddrSegement,  sockaddr_in.sizeof());
        sockaddr_in.sin_family$set(sockaddrSegement, (short) inet_h.AF_INET());
        sockaddr_in.sin_port$set(sockaddrSegement, htons((short) port));
        int res = inet_pton(AF_INET(), session.allocateUtf8String(ipv4), sockaddr_in.sin_addr$slice(sockaddrSegement));
        if (res < 0){
            throw new IllegalStateException("inet_pton fail!,err:"+getNowError());
        }
        return sockaddrSegement;
    }

    public static int setSocket(int fd,int optName){
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment val = session.allocate(JAVA_INT, 1);
            return setsockopt(fd, SOL_SOCKET(), optName, val, (int) val.byteSize());
        }
    }

    public static int getErrorNo(){
        return  errno_h.__errno_location().get(ValueLayout.JAVA_INT, 0);
    }

    public static String getNowError(){
        return getErrorStr(getErrorNo());
    }

    public static String getErrorStr(int errNo){
        return errNo < errStr.length ? errStr[errNo] : "Unknown error "+errNo;
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

    public static String getIpV4Host(MemorySegment sockaddrSegement){
        MemoryAddress memoryAddress = inet_ntoa(sockaddr_in.sin_addr$slice(sockaddrSegement));
        long strlen = strlen(memoryAddress);
        return new String(MemorySegment.ofAddress(memoryAddress, strlen, MemorySession.global()).toArray(JAVA_BYTE));
    }

    public static int getIpv4Port(MemorySegment sockaddrSegement){
        return Short.toUnsignedInt(ntohs(sockaddr_in.sin_port$get(sockaddrSegement)));
    }

    @NativeUnsafe("存在段错误风险")
    public static MemorySegment unsafePointConvertor(MemoryAddress p){
        return MemorySegment.ofAddress(p, Long.MAX_VALUE, MemorySession.global());
    }





    static {
        errStr = new String[257];
        for (int errNo = 0; errNo <= 256; errNo++) {
            MemoryAddress memoryAddress = strerror(errNo);
            long strlen = strlen(memoryAddress);
            MemorySegment memorySegment = MemorySegment.ofAddress(memoryAddress, strlen, MemorySession.global());
            errStr[errNo] = new String(memorySegment.toArray(JAVA_BYTE));
        }
    }



}
