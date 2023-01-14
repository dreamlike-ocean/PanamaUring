package top.dreamlike.access;

import top.dreamlike.async.file.AsyncFile;
import top.dreamlike.async.socket.AsyncServerSocket;
import top.dreamlike.async.socket.AsyncSocket;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.async.uring.IOUringEventLoop;
import top.dreamlike.helper.Unsafe;

import java.util.function.Function;

/**
 * 非导出的符号
 * 走正常的调用 无需使用原来的VarHandler方案
 */
@Unsafe("获取内部的字段值")
public class AccessHelper {
    public static Function<IOUringEventLoop, IOUring> fetchIOURing;

    public static Function<AsyncSocket,Integer> fetchSocketFd;

    public static Function<AsyncFile,Integer> fetchFileFd;

    public static Function<AsyncServerSocket,Integer> fetchServerFd;

    public static Function<AsyncServerSocket,IOUringEventLoop> fetchEventLoop;

}


