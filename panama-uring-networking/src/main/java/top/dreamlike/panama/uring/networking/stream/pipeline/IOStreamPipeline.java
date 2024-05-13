package top.dreamlike.panama.uring.networking.stream.pipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.dreamlike.panama.uring.async.trait.IoUringSocketOperator;
import top.dreamlike.panama.uring.eventloop.IoUringEventLoop;
import top.dreamlike.panama.uring.nativelib.exception.SyscallException;
import top.dreamlike.panama.uring.networking.stream.IOStream;
import top.dreamlike.panama.uring.trait.OwnershipMemory;

import java.lang.foreign.MemorySegment;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class IOStreamPipeline<T extends IoUringSocketOperator> {

    private static final Logger log = LogManager.getLogger(IOStreamPipeline.class);

    private final IoUringEventLoop readerEventLoop;
    private final IoUringEventLoop writerEventLoop;

    private IOContext head;
    private IOContext tail;
    private final IOStream<T> stream;

    private ReentrantReadWriteLock contextListLock = new ReentrantReadWriteLock();

    private final Queue<OwnerShipMemoryProxy> sendQueue;

    private final IoUringSocketOperator socketOperator;

    public IOStreamPipeline(IOStream<T> stream) {
        this(stream, stream.socket().owner());
    }

    public IOStreamPipeline(IOStream<T> stream, IoUringEventLoop writerEventLoop) {
        T socket = stream.socket();
        this.readerEventLoop = socket.owner();
        this.writerEventLoop = writerEventLoop;
        this.stream = stream;
        this.sendQueue = new ArrayDeque<>();
        int fd = socket.fd();
        this.socketOperator = new IoUringSocketOperator() {
            @Override
            public IoUringEventLoop owner() {
                return writerEventLoop;
            }

            @Override
            public int fd() {
                return fd;
            }
        };
        addLast(GuardIOHandle.INSTANCE);
    }

    public IOStream<T> stream() {
        return stream;
    }

    public void addLast(String name, IOHandler handler) {
        IOContext context = new IOContext();
        context.ioHandler = handler;
        context.name = name;
        contextListLock.writeLock().lock();
        try {
            if (head == null) {
                head = context;
            } else {
                tail.after = context;
                context.prev = tail;
            }
            tail = context;
        } finally {
            contextListLock.writeLock().unlock();
        }
        if (handler.executor() == null) {
            readerEventLoop.runOnEventLoop(() -> handler.onHandleAdded(context));
        } else {
            handler.executor().execute(() -> {
                handler.onHandleAdded(context);
            });
        }
    }

    public void addLast(IOHandler handler) {
        addLast(handler.getClass().getSimpleName(), handler);
    }

    public IOHandler find(String name) {
        Objects.requireNonNull(name);
        contextListLock.readLock().lock();
        try {
            IOContext ioContext = findWithoutLock(c -> name.equals(c.name));
            if (ioContext == null) {
                return null;
            }
            return ioContext.ioHandler;
        } finally {
            contextListLock.readLock().unlock();
        }
    }

    public <T extends IOHandler> IOHandler find(Class<T> handleType) {
        Objects.requireNonNull(handleType);
        contextListLock.readLock().lock();
        try {
            IOContext ioContext = findWithoutLock(c -> handleType.equals(c.ioHandler.getClass()));
            if (ioContext == null) {
                return null;
            }
            return ioContext.ioHandler;
        } finally {
            contextListLock.readLock().unlock();
        }
    }

    private IOContext findWithoutLock(Predicate<IOContext> predicate) {
        IOContext context = head;
        while (context != null) {
            if (predicate.test(context)) {
                return context;
            }
            context = context.after;
        }
        return null;
    }

    private boolean addAfterHandler(String indexerName, String name, IOHandler ioHandler) {
        return addAfterHandler(c -> indexerName.equals(c.name), name, ioHandler);
    }

    private <T extends IOHandler> boolean addAfterHandler(Class<T> indexerType, String name, IOHandler ioHandler) {
        return addAfterHandler(c -> c.ioHandler.getClass() == indexerType, name, ioHandler);
    }

    private boolean addAfterHandler(Predicate<IOContext> predicate, String name, IOHandler ioHandler) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(ioHandler);
        Objects.requireNonNull(name);
        IOContext newContext = null;
        contextListLock.writeLock().lock();
        try {

            IOContext target = findWithoutLock(predicate);
            if (target == null) {
                return false;
            }
            newContext = new IOContext();
            newContext.name = name;
            newContext.ioHandler = ioHandler;

            newContext.prev = target;
            newContext.after = target.after;

            target.after = newContext;
            if (newContext.after != null) {
                newContext.after.prev = newContext;
            } else {
                //说明indexer 节点是尾节点
                tail = newContext;
            }
        } finally {
            contextListLock.writeLock().unlock();
        }

        IOContext finalIoContext = newContext;
        if (ioHandler.executor() == null) {
            readerEventLoop.runOnEventLoop(() -> ioHandler.onHandleAdded(finalIoContext));
        } else {
            ioHandler.executor().execute(() -> {
                ioHandler.onHandleAdded(finalIoContext);
            });
        }
        return true;
    }

    public boolean removeHandler(String name) {
        return removeHandler(c -> name.equals(c.name));
    }

    public <T extends IOHandler> boolean removeHandler(Class<T> handleType) {
        return removeHandler(c -> c.ioHandler.getClass() == handleType);
    }

    private boolean removeHandler(Predicate<IOContext> predicate) {
        boolean res = false;
        contextListLock.writeLock().lock();
        IOContext removedContext = null;
        try {
            removedContext = findWithoutLock(predicate);
            if (removedContext == null) {
                return false;
            }

            if (removedContext.prev != null) {
                removedContext.prev.after = removedContext.after;
            } else {
                head = removedContext.after;
            }

            if (removedContext.after != null) {
                removedContext.after.prev = removedContext.prev;
            } else {
                tail = removedContext.prev;
            }

        } finally {
            contextListLock.writeLock().unlock();
        }

        IOContext finalIoContext = removedContext;
        if (removedContext.ioHandler.executor() == IOHandler.EventLoop) {
            readerEventLoop.runOnEventLoop(() -> finalIoContext.ioHandler.onHandleAdded(finalIoContext));
        } else {
            removedContext.ioHandler.executor().execute(() -> {
                finalIoContext.ioHandler.onHandleAdded(finalIoContext);
            });
        }
        return res;
    }

    public void fireRead(Object msg) {
        fireEvent(head, (c) -> c.ioHandler.onRead(c, msg));
    }

    public void fireError(Throwable throwable) {
        fireEvent(head, (c) -> c.ioHandler.onError(c, throwable));
    }

    public void fireWrite(Object msg, CompletableFuture<Void> promise) {
        fireEvent(head, c -> c.ioHandler.onWrite(c, msg, promise));
    }


    private void flushSendQueue(OwnershipMemory msg, CompletableFuture<Void> sendPromise) {
        writerEventLoop.runOnEventLoop(() -> flushSendQueue0(msg, sendPromise));
    }

    private void flushSendQueue0(OwnershipMemory msg, CompletableFuture<Void> sendPromise) {
        OwnerShipMemoryProxy memoryProxy = sendQueue.peek();
        OwnerShipMemoryProxy sendTask = new OwnerShipMemoryProxy(msg, sendPromise);
        if (memoryProxy != null) {
            //当前有数据正在发送
            sendQueue.offer(sendTask);
            return;
        }
        sendQueue.offer(sendTask);
        send0();
    }

    private void send0() {
        OwnerShipMemoryProxy currentSendTask = sendQueue.peek();
        OwnershipMemory sendBuffer = currentSendTask.toOwnershipMemory();
        socketOperator.asyncSend(sendBuffer, (int) sendBuffer.resource().byteSize(), 0)
                .whenComplete((br, t) -> {
                    if (t != null) {
                        fireError(t);
                        return;
                    }
                    int i = br.syscallRes();
                    if (i < 0) {
                        fireError(new SyscallException(i));
                        return;
                    }
                    int hasSend = i;
                    currentSendTask.hasSend += hasSend;
                    if (currentSendTask.hasSend == currentSendTask.total) {
                        currentSendTask.drop();
                    } else {
                        send0();
                    }
                });

    }

    private void fireEvent(IOContext context, Consumer<IOContext> handle) {
        if (context == null) {
            return;
        }
        Runnable r = () -> {
            try {
                handle.accept(context);
            } catch (Throwable e) {
                context.fireNextError(e);
            }
        };
        if (context.ioHandler.executor() == IOHandler.EventLoop) {
            readerEventLoop.runOnEventLoop(r);
        } else {
            context.ioHandler.executor().execute(r);
        }
    }

    public class IOContext {
        IOHandler ioHandler;
        IOContext prev;
        IOContext after;

        String name;


        public void fireNextHandleInactive() {
            fireEvent(after, c -> c.ioHandler.onHandleInactive(c));
        }

        public void fireNextRead(Object msg) {
            if (after == null) {
                if (msg instanceof OwnershipMemory memory) {
                    memory.drop();
                    log.error("msg is OwnershipMemory but next is null, drop it");
                }
                return;
            }
            fireEvent(after, c -> c.ioHandler.onRead(c, msg));
        }

        public void fireNextWrite(Object msg, CompletableFuture<Void> writePrmoise) {
            if (after == null) {
                if (msg instanceof OwnershipMemory memory) {
                    //下一个没了 说明当前是最后一个准备直接写出
                    flushSendQueue(memory, writePrmoise);
                } else {
                    fireError(new IllegalArgumentException("msg is not OwnershipMemory"));
                }
                return;
            }
            fireEvent(after, c -> c.ioHandler.onWrite(c, msg, writePrmoise));
        }

        public void fireNextError(Throwable cause) {
            fireEvent(after, c -> c.ioHandler.onError(c, cause));
        }


    }


    private static class OwnerShipMemoryProxy implements OwnershipMemory {
        private final OwnershipMemory originalMemory;
        long hasSend;
        long total;
        private final CompletableFuture<Void> sendPromise;

        private OwnerShipMemoryProxy(OwnershipMemory originalMemory, CompletableFuture<Void> sendPromise) {
            this.originalMemory = originalMemory;
            this.hasSend = 0L;
            this.total = originalMemory.resource().byteSize();
            this.sendPromise = sendPromise;
        }

        @Override
        public MemorySegment resource() {
            MemorySegment waitToSend = originalMemory.resource();
            waitToSend = waitToSend.asSlice(hasSend, total - hasSend);
            return waitToSend;
        }

        public OwnershipMemory toOwnershipMemory() {
            return OwnershipMemory.of(resource());
        }

        @Override
        public void drop() {
            originalMemory.drop();
            sendPromise.complete(null);
        }
    }

    private static class GuardIOHandle implements IOHandler {
        public static final GuardIOHandle INSTANCE = new GuardIOHandle();
        @Override
        public Executor executor() {
            return IOHandler.EventLoop;
        }
    }
}
