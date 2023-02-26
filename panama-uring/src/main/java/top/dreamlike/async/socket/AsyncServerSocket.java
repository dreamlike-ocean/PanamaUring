package top.dreamlike.async.socket;

import top.dreamlike.access.AccessHelper;
import top.dreamlike.async.AsyncFd;
import top.dreamlike.async.socket.extension.IOUringFlow;
import top.dreamlike.async.uring.IOUring;
import top.dreamlike.eventloop.IOUringEventLoop;
import top.dreamlike.extension.NotEnoughSqeException;
import top.dreamlike.helper.NativeHelper;
import top.dreamlike.helper.SocketInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public non-sealed class AsyncServerSocket extends AsyncFd {

    protected final IOUring uring;
    protected final int serverFd;


    public AsyncServerSocket(IOUringEventLoop eventLoop, String host, int port) {
        super(eventLoop);
        this.uring = AccessHelper.fetchIOURing.apply(eventLoop);
        this.serverFd = NativeHelper.serverListen(host, port);
    }


    //受限于java泛型限制
    public CompletableFuture<? extends AsyncSocket> accept() {
        CompletableFuture<SocketInfo> res = new CompletableFuture<>();
        eventLoop.runOnEventLoop(() -> {
            if (!uring.prep_accept(serverFd, res::complete)) {
                res.completeExceptionally(new NotEnoughSqeException());
            }
        });
        return res
                .thenCompose(s -> NativeHelper.errorNoTransformGeneric(s.fd(), s))
                .thenApply(si -> new AsyncSocket(si.fd(), si.host(), si.port(), eventLoop));
    }

    public Flow.Subscription acceptMulti(Consumer<AsyncSocket> socketCallBack) {
        //下面的注释中的数字为 时间序列中的控制流顺序

        //1
        IOUringFlow<AsyncSocket> flow = new IOUringFlow<>(Integer.MAX_VALUE, eventLoop, socketCallBack);
        //2 切到eventLoop线程
        eventLoop.runOnEventLoop((promise) -> {
            //4 初始化sqe和挂载回调
            // 这个回调是cqe收割时的
            long opt = uring.prep_accept_multi_and_get_user_data(serverFd, (socketInfo) -> {
                //7cqe回调回来
                AsyncSocket res = new AsyncSocket(socketInfo.fd(), socketInfo.host(), socketInfo.port(), eventLoop);
                //塞流里面
                //8 发送事件触发socketCallBack 由于是单线程实现所以这里还是同步的
                boolean allowContinue = flow.offer(res);
                // 满了 受被压限制 直接取消
                if (!allowContinue) {
                    //9 异步取消
                    flow.cancel();
                }
            });
            //5 校验初始化参数返回值结果
            if (opt < 0) {
                promise.completeExceptionally(new NotEnoughSqeException());
                return;
            }
            //6 初始化 Subscription
            flow.setUserData(opt);
            promise.complete(opt);
        });

        //3 retutn
        return flow;
    }

    static {
        AccessHelper.fetchEventLoop = server -> server.eventLoop;
        AccessHelper.fetchServerFd = server -> server.serverFd;
    }

    @Override
    public IOUringEventLoop fetchEventLoop() {
        return eventLoop;
    }
}
