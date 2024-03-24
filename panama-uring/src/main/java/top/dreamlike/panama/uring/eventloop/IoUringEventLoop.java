package top.dreamlike.panama.uring.eventloop;

import org.jctools.queues.MpscUnboundedArrayQueue;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.panama.uring.async.cancel.CancelToken;
import top.dreamlike.panama.uring.async.cancel.IoUringCancelToken;
import top.dreamlike.panama.uring.fd.EventFd;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.libs.LibUring;
import top.dreamlike.panama.uring.nativelib.struct.liburing.*;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;
import top.dreamlike.panama.uring.thirdparty.colletion.LongObjectHashMap;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant.IORING_CQE_F_MORE;

public class IoUringEventLoop extends Thread implements AutoCloseable, Executor {
    private static final LibUring libUring = Instance.LIB_URING;
    private static final int cqeSize = 4;
    private final EventFd wakeUpFd;
    private final AtomicBoolean inWait;
    private final AtomicBoolean hasClosed;

    private IoUring internalRing;

    private Arena singleThreadArena;

    private MpscUnboundedArrayQueue<Runnable> taskQueue;

    private MemorySegment cqes;

    private final AtomicLong tokenGenerator;

    private final LongObjectHashMap<Consumer<IoUringCqe>> callBackMap;

    private final PriorityQueue<ScheduledTask> scheduledTasks;

    private KernelTime64Type kernelTime64Type;

    public IoUringEventLoop(Function<SegmentAllocator, IoUringParams> ioUringParamsFactory) {
        this.wakeUpFd = new EventFd(0, 0);
        this.inWait = new AtomicBoolean(false);
        this.taskQueue = new MpscUnboundedArrayQueue<>(1024);
        this.hasClosed = new AtomicBoolean();
        this.tokenGenerator = new AtomicLong(0);
        this.callBackMap = new LongObjectHashMap<>();
        this.taskQueue.add(() -> initRing(ioUringParamsFactory));
        this.scheduledTasks = new PriorityQueue<>();
    }

    private void initRing(Function<SegmentAllocator, IoUringParams> ioUringParamsFactory) {
        this.singleThreadArena = Arena.ofConfined();
        this.internalRing = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, IoUring.class);
        IoUringParams ioUringParams = ioUringParamsFactory.apply(singleThreadArena);
        int initRes = Instance.LIB_URING.io_uring_queue_init_params(ioUringParams.getSq_entries(), internalRing, ioUringParams);
        if (initRes < 0) {
            throw new IllegalArgumentException(STR."io_uring_queue_init_params error,error Reason: \{DebugHelper.getErrorStr(-initRes)}");
        }
        this.cqes = singleThreadArena.allocate(IoUringConstant.AccessShortcuts.IoUringCqeLayout, cqeSize);
        this.kernelTime64Type = Instance.STRUCT_PROXY_GENERATOR.allocate(singleThreadArena, KernelTime64Type.class);
    }

    @Override
    public void run() {
        while (hasClosed.get()) {
            inWait.set(true);
            while (true) {
                ScheduledTask next = scheduledTasks.peek();
                if (next != null && next.deadlineNanos <= System.nanoTime()) {
                    taskQueue.offer(scheduledTasks.poll().task);
                } else {
                    break;
                }
            }
            while (!taskQueue.isEmpty()) {
                taskQueue.poll().run();
            }
            ScheduledTask nextTask = scheduledTasks.peek();
            //如果没有任务或者下一个任务的时间大于当前时间，那么就直接提交
            if (nextTask == null || nextTask.deadlineNanos > System.nanoTime()) {
                inWait.set(false);
                if (nextTask == null) {
                    libUring.io_uring_submit_and_wait(internalRing, 1);
                } else {
                    long duration = nextTask.deadlineNanos - System.nanoTime();
                    kernelTime64Type.setTv_sec(duration / 1000000000);
                    kernelTime64Type.setTv_nsec(duration % 1000000000);
                    libUring.io_uring_submit_and_wait_io_uring_wait_cqes(internalRing, cqes, cqeSize, kernelTime64Type, null);
                }
            }
        }
        releaseResource();
    }

    public CancelToken fillTemplate(Consumer<IoUringSqe> sqeFunction, Consumer<IoUringCqe> callback) {
        long token = tokenGenerator.getAndIncrement();
        Runnable r = () -> {
            IoUringSqe sqe = ioUringGetSqe();
            sqeFunction.accept(sqe);
            sqe.setUser_data(token);
            callBackMap.put(token, callback);
        };
        if (inEventLoop()) {
            r.run();
        } else {
            execute(r);
        }
        return new IoUringCancelToken(this, token);
    }

    private IoUringSqe ioUringGetSqe() {
        IoUringSqe sqe = libUring.io_uring_get_sqe(internalRing);
        //fast_sqe
        if (sqe != null) {
            return sqe;
        }
        flush();
        return libUring.io_uring_get_sqe(internalRing);
    }

    public void flush()  {
        if (inEventLoop()) {
            libUring.io_uring_submit(internalRing);
        }else {
            execute(this::flush);
        }
    }


    private void processCqes() {
        //todo wake fd的事情
        int count = libUring.io_uring_peek_batch_cqe(internalRing, cqes, cqeSize);
        for (int i = 0; i < count; i++) {
            IoUringCqe nativeCqe = Instance.STRUCT_PROXY_GENERATOR.enhance(cqes.getAtIndex(ValueLayout.ADDRESS, i));
            long token = nativeCqe.getUser_data();
            boolean multiShot = (nativeCqe.getFlags() & IORING_CQE_F_MORE) != 0;
            Consumer<IoUringCqe> callback = multiShot ? callBackMap.get(token) : callBackMap.remove(token);
            if (callback != null) {
                callback.accept(nativeCqe);
            }
        }
        libUring.io_uring_cq_advance(internalRing, count);
    }


    @Override
    public void close() throws Exception {
        hasClosed.compareAndSet(false, true);
    }

    private void releaseResource() {
        this.wakeUpFd.close();
        Instance.LIB_URING.io_uring_queue_exit(internalRing);
        this.singleThreadArena.close();
    }

    private boolean inEventLoop() {
        return Thread.currentThread() == this;
    }

    @Override
    public void execute(Runnable command) {
        if (hasClosed.get()) {
            throw new IllegalStateException("EventLoop has closed");
        }
        taskQueue.add(command);
        if (inWait.compareAndSet(false, true)) {
            wakeUpFd.eventfdWrite(1);
        }
    }

    public void submitScheduleTask(long delay, TimeUnit timeUnit, Runnable task) {
        long deadlineNanos = System.nanoTime() + timeUnit.toNanos(delay);
        execute(() -> scheduledTasks.offer(new ScheduledTask(deadlineNanos, task)));
    }

    private record ScheduledTask(long deadlineNanos, Runnable task) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask o) {
            return Long.compare(deadlineNanos, o.deadlineNanos);
        }
    }
}
