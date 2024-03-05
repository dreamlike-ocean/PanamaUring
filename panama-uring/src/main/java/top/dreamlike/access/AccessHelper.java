package top.dreamlike.access;

import top.dreamlike.async.IOOpResult;
import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.AsyncServerSocket;
import top.dreamlike.async.AsyncSocket;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.epoll.Epoll;
import top.dreamlike.eventloop.EpollEventLoop;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.fileSystem.WatchService;
import top.dreamlike.helper.Pair;
import top.dreamlike.helper.Unsafe;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

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


    public static BiConsumer<EpollEventLoop, Queue<Runnable>> setEpollEventLoopTasks;

    public static BiConsumer<EpollEventLoop, Pair<Epoll.Event, IntConsumer>> registerToEpollDirectly;


    public static Function<IOUring, Map<Long, IOOpResult>> fetchContext;

}


