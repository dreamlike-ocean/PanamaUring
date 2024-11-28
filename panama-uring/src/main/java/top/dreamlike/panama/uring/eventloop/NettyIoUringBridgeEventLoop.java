package top.dreamlike.panama.uring.eventloop;

import io.netty.channel.IoEvent;
import io.netty.channel.IoHandle;
import io.netty.channel.IoRegistration;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.uring.IoUringIoEvent;
import io.netty.channel.uring.IoUringIoHandle;
import io.netty.channel.uring.IoUringIoOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.dreamlike.panama.uring.helper.NettyHelper;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringParams;

import java.lang.foreign.ValueLayout;
import java.util.function.Consumer;

public class NettyIoUringBridgeEventLoop extends AbstractNettyBridgeEventLoop implements IoUringIoHandle {

    private static final Logger log = LoggerFactory.getLogger(NettyIoUringBridgeEventLoop.class);

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
            registration.submit(
                    new IoUringIoOps(
                            IoUringConstant.Opcode.IORING_OP_READ,
                            0, (short) 0, nettyFd.intValue(), 0,
                            cqeReadyMemory.address(), (int)ValueLayout.JAVA_LONG.byteSize(),
                            0, (short) 0
                    )
            );
            log.debug("end setRegistration");
        } catch (Exception e) {
            log.error("register epoll bridge event loop failed", e);
        }
    }

    @Override
    protected IoHandle ioHandle() {
        return this;
    }

    @Override
    public void handle(IoRegistration ioRegistration, IoEvent ioEvent) {
        log.debug("Netty eventLoop find cqe eventFd is readable");
        IoUringIoEvent nettyIoUringEvent = (IoUringIoEvent) ioEvent;
        byte opCode = nettyIoUringEvent.opcode();
        if (opCode != IoUringConstant.Opcode.IORING_OP_READ) {
            log.debug("skip handle cqe");
            return;
        }

        //处理cqe
        ioUringCore.processCqes(this::processCqes);
        //重新注册可读事件
        registerReadyFdOneShot();
    }

    static {
        if (!NettyHelper.isIoUringSupported) {
            throw new IllegalStateException("please import netty-transport-native-io_uring");
        }
    }
}
