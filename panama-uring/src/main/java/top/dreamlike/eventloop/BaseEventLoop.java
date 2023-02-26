package top.dreamlike.eventloop;

import org.jctools.queues.MpscLinkedQueue;

import java.time.Duration;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class BaseEventLoop extends Thread implements Executor {

    protected Queue<Runnable> tasks;

    protected Thread worker;

    protected AtomicBoolean close;
    protected PriorityQueue<TimerTask> timerTasks;

    protected AtomicBoolean wakeUpFlag;

    public BaseEventLoop() {
        worker = this;
        tasks = new MpscLinkedQueue<>();
        timerTasks = new PriorityQueue<>(Comparator.comparingLong(TimerTask::getDeadline));
        close = new AtomicBoolean(false);
        wakeUpFlag = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        while (!close.get()) {
            long duration = -1;
            TimerTask peek = timerTasks.peek();
            if (peek != null) {
                // 过期任务
                if (peek.deadline <= System.currentTimeMillis()) {
                    runAllTimerTask();
                }
                // 最近的一个定时任务
                if (timerTasks.peek() != null) {
                    duration = timerTasks.peek().deadline - System.currentTimeMillis();
                }
            }
            while (!tasks.isEmpty()) {
                tasks.poll().run();
            }
            wakeUpFlag.set(false);
            selectAndWait(duration);
            wakeUpFlag.set(true);
            // 到期的任务
            afterSelect();
        }
        try {
            close0();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            afterClose();
        }
    }

    protected void runAllTimerTask() {
        while (!timerTasks.isEmpty() && timerTasks.peek().deadline <= System.currentTimeMillis()) {
            TimerTask timerTask = timerTasks.poll();
            timerTask.runnable.run();
        }
    }

    public void runOnEventLoop(Runnable runnable) {
        if (inEventLoop()) {
            runnable.run();
            return;
        }
        execute(runnable);
    }


    public <T> CompletableFuture<T> runOnEventLoop(Consumer<CompletableFuture<T>> handler) {
        CompletableFuture<T> promise = new CompletableFuture<>();
        if (inEventLoop()) {
            handler.accept(promise);
        } else {
            execute(() -> handler.accept(promise));
        }
        return promise;
    }

    public boolean inEventLoop() {
        return Thread.currentThread() == this;
    }

    @Override
    public void execute(Runnable command) {
        if (close.get()) {
            throw new IllegalStateException("eventloop has closed");
        }
        tasks.offer(command);
        wakeup();
    }

    public CompletableFuture<Void> scheduleTask(Runnable runnable, Duration duration) {
        if (close.get()) {
            throw new IllegalStateException("eventloop has closed");
        }

        CompletableFuture<Void> res = new CompletableFuture<>();
        runOnEventLoop(() -> {
            long millis = duration.toMillis();
            TimerTask task = new TimerTask(() -> {
                runnable.run();
                res.complete(null);
            }, System.currentTimeMillis() + millis);
            task.future = res;
            timerTasks.offer(task);
        });
        return res;
    }

    public <T> CompletableFuture<T> runOnEventLoop(Callable<T> fn) {
        CompletableFuture<T> res = new CompletableFuture<>();
        runOnEventLoop(() -> {
            try {
                res.complete(fn.call());
            } catch (Exception e) {
                res.completeExceptionally(e);
            }
        });
        return res;
    }

    public abstract void wakeup();

    protected abstract void selectAndWait(long duration);

    protected abstract void close0() throws Exception;

    protected void afterClose() {

    }

    protected abstract void afterSelect();

    public static class TimerTask {
        public Runnable runnable;
        // 绝对值 ms
        public long deadline;

        public CompletableFuture future;

        public TimerTask(Runnable runnable, long deadline) {
            this.runnable = runnable;
            this.deadline = deadline;
        }

        public long getDeadline() {
            return deadline;
        }
    }

}
