package top.dreamlike.panama.uring.async.fd;

import top.dreamlike.panama.uring.async.IoUringSyscallResult;
import top.dreamlike.panama.uring.async.cancel.CancelableFuture;
import top.dreamlike.panama.uring.async.trait.IoUringBufferRing;
import top.dreamlike.panama.uring.async.trait.IoUringOperator;
import top.dreamlike.panama.uring.helper.LambdaHelper;
import top.dreamlike.panama.uring.helper.PanamaUringSecret;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBufferRingElement;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.sync.trait.NativeFd;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface IoUringSelectedReadableFd extends IoUringOperator, NativeFd {
    IoUringBufferRing bufferRing();

    default CancelableFuture<OwnershipMemory> asyncSelectedRead(int len, int offset) {
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<OwnershipMemory>) owner().asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
                    sqe.setBufGroup(bufferRing.getBufferGroupId());
                })
                .thenComposeAsync(cqe -> {
                    int syscallResult = cqe.getRes();
                    if (syscallResult < 0) {
                        return CompletableFuture.failedFuture(new SyscallException(syscallResult));
                    } else {
                        int readLen = syscallResult;
                        int bid = cqe.getBid();
                        IoUringBufferRingElement ringElement = bufferRing.removeBuffer(bid).resultNow();
                        return CompletableFuture.completedFuture(borrowUringBufferRingElement(ringElement, readLen));
                    }
                }, r -> owner().runOnEventLoop(r));
    }

    default CancelableFuture<IoUringSyscallResult<OwnershipMemory>> asyncSelectedReadResult(int len, int offset) {
        IoUringBufferRing bufferRing = Objects.requireNonNull(bufferRing());
        return (CancelableFuture<IoUringSyscallResult<OwnershipMemory>>) owner().asyncOperation(sqe -> {
                    Instance.LIB_URING.io_uring_prep_read(sqe, readFd(), MemorySegment.NULL, len, offset);
                    sqe.setFlags((byte) (sqe.getFlags() | IoUringConstant.IOSQE_BUFFER_SELECT));
                    sqe.setBufGroup(bufferRing.getBufferGroupId());
                })
                .thenApplyAsync(cqe -> {
                    //强制制定在eventloop上 防止外部get导致切换线程
                    IoUringSyscallResult<OwnershipMemory> result;
                    if (cqe.getRes() < 0) {
                        result = new IoUringSyscallResult<>(cqe.getRes(), OwnershipMemory.of(MemorySegment.NULL));
                    } else {
                        int readLen = cqe.getRes();
                        int bid = cqe.getBid();
                        IoUringBufferRingElement ringElement = bufferRing.removeBuffer(bid).resultNow();
                        result = new IoUringSyscallResult<>(cqe.getRes(), borrowUringBufferRingElement(ringElement, readLen));
                    }
                    return result;
                }, r -> owner().runOnEventLoop(r));

    }

    static OwnershipMemory borrowUringBufferRingElement(IoUringBufferRingElement ringElement, int len) {
        return new OwnershipBufferRingElement(ringElement, len);
    }
}

class OwnershipBufferRingElement implements OwnershipMemory {

    private static final VarHandle ELEMENT_VH;

    private IoUringBufferRingElement element;

    private final int len;

    OwnershipBufferRingElement(IoUringBufferRingElement element, int len) {
        this.element = element;
        this.len = len;
    }

    @Override
    public MemorySegment resource() {
        if (element == null) {
            throw new IllegalStateException("element has been released");
        }
        IoUringBufferRingElement ringElement = (IoUringBufferRingElement) ELEMENT_VH.getVolatile(this);
        if (ringElement == null) {
            throw new IllegalStateException("element has been released");
        }
        return ringElement.element().reinterpret(len);
    }

    @Override
    public void drop() {
        IoUringBufferRingElement waitToRelease = (IoUringBufferRingElement) ELEMENT_VH.compareAndExchange(this, element, (IoUringBufferRingElement) null);
        if (waitToRelease == null) {
            return;
        }
        CompletableFuture<Void> buffer = waitToRelease.ring().releaseBuffer(waitToRelease);
        if (!waitToRelease.ring().owner().inEventLoop()) {
            LambdaHelper.runWithThrowable(buffer::get);
        }
    }

    static {
        try {
            ELEMENT_VH = MethodHandles.lookup()
                    .findVarHandle(OwnershipBufferRingElement.class, "element", IoUringBufferRingElement.class)
                    .withInvokeExactBehavior();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        PanamaUringSecret.lookupOwnershipBufferRingElement = (e) -> ((OwnershipBufferRingElement) e).element;
    }
}