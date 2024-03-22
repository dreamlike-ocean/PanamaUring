import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.common.CType;
import top.dreamlike.panama.nativelib.Instance;
import top.dreamlike.panama.nativelib.struct.epoll.EpollData;
import top.dreamlike.panama.nativelib.struct.epoll.EpollEvent;
import top.dreamlike.panama.nativelib.struct.sigset.SigsetType;
import top.dreamlike.panama.nativelib.struct.socket.MsgHdr;
import top.dreamlike.panama.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.*;
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
    public void testMsgHdr() {
        MemoryLayout layout = Instance.STRUCT_PROXY_GENERATOR.extract(MsgHdr.class);
        final GroupLayout generatedLayout = MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("msg_name"),
                JAVA_INT.withName("msg_namelen"),
                MemoryLayout.paddingLayout(4),

                ADDRESS.withName("msg_iov"),
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
        MemoryLayout eventLayout = Instance.STRUCT_PROXY_GENERATOR.extract(EpollEvent.class);
        MemorySegment epollEventMemory = Arena.global().allocate(eventLayout);
        EpollEvent epollEvent = Instance.STRUCT_PROXY_GENERATOR.enhance(epollEventMemory);
        epollEvent.setU64(1024);
        Assert.assertEquals(1024, epollEvent.getData().getU64());
    }
}
