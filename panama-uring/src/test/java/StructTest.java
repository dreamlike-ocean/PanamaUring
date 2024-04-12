import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.helper.DebugHelper;
import top.dreamlike.panama.uring.nativelib.struct.epoll.NativeEpollEvent;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringBuf;
import top.dreamlike.panama.uring.nativelib.struct.liburing.IoUringConstant;
import top.dreamlike.panama.uring.nativelib.struct.liburing.NativeIoUringBufRing;
import top.dreamlike.panama.uring.nativelib.struct.sigset.SigsetType;
import top.dreamlike.panama.uring.nativelib.struct.socket.MsgHdr;
import top.dreamlike.panama.uring.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.foreign.ValueLayout.*;

public class StructTest {

    @Test
    public void testOther() {
        StructLayout sigset_tLayout = MemoryLayout.structLayout(
                MemoryLayout.sequenceLayout(16, JAVA_LONG).withName("__val")
        );

        MemoryLayout sigsetTypeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(SigsetType.class);
        Assert.assertEquals(sigset_tLayout, sigsetTypeLayout);
        Assert.assertEquals(sigset_tLayout.byteSize(), sigsetTypeLayout.byteSize());

        MemoryLayout time64TypeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(KernelTime64Type.class);

        StructLayout __kernel_timespec_layout = MemoryLayout.structLayout(
                JAVA_LONG.withName("tv_sec"),
                JAVA_LONG.withName("tv_nsec")
        );

        Assert.assertEquals(__kernel_timespec_layout, time64TypeLayout);
        Assert.assertEquals(__kernel_timespec_layout.byteSize(), time64TypeLayout.byteSize());
    }

    @Test
    public void testOrder() {
        short a = 329;
        short current = DebugHelper.htons(a);
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer = buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(0, a);
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        short expected = buffer.getShort(0);
        Assert.assertEquals(current,expected);
    }

    @Test
    public void testMsgHdr() {
        MemoryLayout layout = Instance.STRUCT_PROXY_GENERATOR.extract(MsgHdr.class);
        final GroupLayout generatedLayout = MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("msg_name"),
                JAVA_INT.withName("msg_namelen"),
                MemoryLayout.paddingLayout(4),

                ADDRESS.withName("msg_iov").withTargetLayout(MemoryLayout.structLayout(
                                ADDRESS.withName("iov_base"),
                                JAVA_LONG.withName("iov_len")
                        )
                )
                ,
                JAVA_LONG.withName("msg_iovlen"),

                ADDRESS.withName("msg_control"),
                JAVA_LONG.withName("msg_controllen"),

                JAVA_INT.withName("msg_flags"),
                MemoryLayout.paddingLayout(4)
        );
        Assert.assertEquals(generatedLayout, layout);
        Assert.assertEquals(generatedLayout.byteSize(), layout.byteSize());
    }

    @Test
    public void testEpoll() {
        MemoryLayout eventLayout = Instance.STRUCT_PROXY_GENERATOR.extract(NativeEpollEvent.class);
        MemorySegment epollEventMemory = Arena.global().allocate(eventLayout);
        NativeEpollEvent nativeEpollEvent = Instance.STRUCT_PROXY_GENERATOR.enhance(epollEventMemory);
        nativeEpollEvent.setU64(1024);
        Assert.assertEquals(1024, nativeEpollEvent.getData().getU64());
    }

    @Test
    public void testIoUringBufRingStruct() {
        NativeIoUringBufRing buf = Instance.STRUCT_PROXY_GENERATOR.allocate(Arena.global(), NativeIoUringBufRing.class);
        MemorySegment memorySegment = StructProxyGenerator.findMemorySegment(buf);
        Assert.assertEquals(memorySegment.address(), buf.getBufs().address());
        VarHandle tailVarhandle = IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_VARHANDLE;
        JAVA_SHORT.varHandle()
                .setRelease(memorySegment, IoUringConstant.AccessShortcuts.IO_URING_BUF_RING_TAIL_OFFSET, (short) 12);
        short i = (short) tailVarhandle.get(memorySegment, 0L);
        Assert.assertEquals(12, i);

        MemoryLayout memoryLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringBuf.class);
        MemorySegment segment = Arena.global().allocate(memoryLayout, 2);
        IoUringConstant.AccessShortcuts.IO_URING_BUF_LEN_VARHANDLE.set(segment, memoryLayout.byteSize(), 123);
        IoUringBuf atIndex1 = Instance.STRUCT_PROXY_GENERATOR.enhance(segment.asSlice(memoryLayout.byteSize(), memoryLayout.byteSize()));
        Assert.assertEquals(123, atIndex1.getLen());

        StructLayout newLayout = MemoryLayout.structLayout(
                JAVA_SHORT.withName("a")
        );
        VarHandle vh = newLayout.varHandle(PathElement.groupElement("a"));
        segment = Arena.global().allocate(newLayout);
        vh.set(segment, 0L, (short) 1);
    }
}
