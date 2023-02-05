package top.dreamlike.access;

import top.dreamlike.FileSystem.WatchService;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.helper.Unsafe;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 非导出的符号
 * 走正常的调用 无需使用原来的VarHandler方案
 */
@Unsafe("获取内部的字段值")
public class AccessHelper {
    public static Function<IOUringEventLoop, IOUring> fetchIOURing;

    public static Function<AsyncSocket, Integer> fetchSocketFd;

    public static Function<AsyncFile, Integer> fetchFileFd;

    public static Function<AsyncServerSocket, Integer> fetchServerFd;

    public static Function<AsyncServerSocket, IOUringEventLoop> fetchEventLoop;

    public static Function<WatchService, Integer> fetchINotifyFd;

    public static Consumer<Epoll> registerStdInput;

    public static Consumer<Epoll> unregisterStdInput;

    public static Function<WatchService, MemorySegment> fetchBuffer;

}


