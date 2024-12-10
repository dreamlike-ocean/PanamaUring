package top.dreamlike.panama.uring.eventloop;

import io.netty.channel.IoEvent;
import io.netty.channel.IoRegistration;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.uring.IoUringIoEvent;
import io.netty.channel.uring.IoUringIoHandle;
import io.netty.channel.uring.IoUringIoOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.NettyHelper;
import top.dreamlike.panama.uring.nativelib.libs.Libc;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

public class NettyIoUringBridgeEventLoop extends AbstractNettyBridgeEventLoop implements IoUringIoHandle {

    private static final Logger log = LoggerFactory.getLogger(NettyIoUringBridgeEventLoop.class);

    static {
        if (!NettyHelper.isIoUringSupported) {
            throw new IllegalStateException("please import netty-transport-native-io_uring");
        }
    }

    private long nettyIoUringReadId = 0;

    public NettyIoUringBridgeEventLoop(SingleThreadIoEventLoop eventLoop, Consumer<IoUringParams> ioUringParamsFactory) {
        super(eventLoop, ioUringParamsFactory);
    }

    @Override
    protected void setRegistration0(IoRegistration registration) {
        registerReadyFdOneShot();
    }

    private void registerReadyFdOneShot() {
        try {
            //不用清空buffer 覆盖写入就行了 无所谓 不关心这些
            this.nettyIoUringReadId = registration.submit(
                    new IoUringIoOps(
                            IoUringConstant.Opcode.IORING_OP_READ,
                            0, (short) 0, nettyFd.intValue(), 0,
                            cqeReadyMemory.address(), (int) ValueLayout.JAVA_LONG.byteSize(),
                            0, (short) 0
                    )
            );
            log.debug("end setRegistration");
        } catch (Exception e) {
            log.error("register epoll bridge event loop failed", e);
        }
    }

    @Override
    protected IoUringIoHandle ioHandle() {
        return this;
    }

    @Override
    public void handle(IoRegistration ioRegistration, IoEvent ioEvent) {
        log.debug("Netty eventLoop find cqe eventFd is readable");
        IoUringIoEvent nettyIoUringEvent = (IoUringIoEvent) ioEvent;
        byte opCode = nettyIoUringEvent.opcode();
        if (opCode != IoUringConstant.Opcode.IORING_OP_READ && opCode != IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL) {
            log.debug("skip handle cqe");
            return;
        }

        try {
            int res = nettyIoUringEvent.res();
            if (opCode == IoUringConstant.Opcode.IORING_OP_READ) {
                //关闭时出现的cqe 告诉被取消了
                //如果不判断就去processCqes会导致摸到被close的io uring fd的被mumap的cq
                //导致coredump
                if (res == -Libc.Error_H.ECANCELED) {
                    log.debug("eventLoop is closed");
                    releaseResource();
                    return;
                }
                //处理cqe
                ioUringCore.processCqes(this::processCqes);
                return;
            }

            if (opCode == IoUringConstant.Opcode.IORING_OP_ASYNC_CANCEL && res == -Libc.Error_H.EALREADY) {
                registration.submit(IoUringIoOps.newAsyncCancel(nettyFd.intValue(), 0, nettyIoUringReadId, IoUringConstant.Opcode.IORING_OP_READ));
            }
        } catch (Exception e) {
            //should not reach hear
            throw new RuntimeException(e);
        } finally {
            if (!hasClosed.get()) {
                registerReadyFdOneShot();
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (hasClosed.compareAndSet(false, true)) {
            registration.submit(IoUringIoOps.newAsyncCancel(nettyFd.intValue(), 0, nettyIoUringReadId, IoUringConstant.Opcode.IORING_OP_READ));
        }
    }
}
