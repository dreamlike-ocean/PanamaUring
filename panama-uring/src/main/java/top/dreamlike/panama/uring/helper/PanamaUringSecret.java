package top.dreamlike.panama.uring.helper;

import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.fd.AsyncMultiShotTcpSocketFd;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUring;
import top.dreamlike.panama.uring.nativelib.wrapper.IoUringCore;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.util.function.Function;

public class PanamaUringSecret {

    public static Function<CancelToken, Long> peekCancelToken;

    public static Function<IoUringEventLoop, IoUring> findUring;

    public static Function<AsyncMultiShotTcpSocketFd,  IoUringBufferRing> findBufferRing;

    public static Function<IoUringEventLoop, IoUringCore> findIoUringCore;

    public static Function<IoUringCore, Integer> getCqSizeFromCore;

    public static Function<IoUringEventLoop, Integer> getCqSize = loop -> getCqSizeFromCore.apply(findIoUringCore.apply(loop));
}
