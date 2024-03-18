import org.junit.Assert;
import org.junit.Test;
import struct.io_uring_cqe_struct;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.nativelib.Instance;
import top.dreamlike.panama.nativelib.struct.liburing.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

public class LiburingTest {


    @Test
    public void testLayout() {
        MemoryLayout sqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class).withName("io_uring_sqe");
        Assert.assertEquals(io_uring_sqe_struct.layout.byteSize(), sqeLayout.byteSize());
        Assert.assertEquals(io_uring_sqe_struct.layout, sqeLayout);


        MemoryLayout cqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCqe.class).withName("io_uring_cqe");
        Assert.assertEquals(io_uring_cqe_struct.layout.byteSize(), cqeLayout.byteSize());
        Assert.assertEquals(io_uring_cqe_struct.layout, cqeLayout);


        VarHandle handle = sqeLayout.varHandle(
                MemoryLayout.PathElement.groupElement("flagsUnion"),
                MemoryLayout.PathElement.groupElement("rw_flags")
        );
        MemorySegment memorySegment = Arena.global().allocate(sqeLayout.byteSize());
        handle.set(memorySegment, 0, 100);
        int flag = (int) handle.get(memorySegment, 0);
        IoUringSqe sqe = Instance.STRUCT_PROXY_GENERATOR.enhance(memorySegment);
        int rwFlags = sqe.getFlagsUnion().getRw_flags();
        Assert.assertEquals(flag, rwFlags);
        sqe.setOffset(1024L);
        Assert.assertEquals(1024L, sqe.getOffset());
        Assert.assertEquals(1024L, sqe.getOffsetUnion().getOff());
        sqe.setAddr(2048);
        Assert.assertEquals(2048, sqe.getAddr());
        Assert.assertEquals(2048, sqe.getBufferUnion().getAddr());
        sqe.setFlagsInFlagsUnion(4);
        Assert.assertEquals(4, sqe.getRwFlags());
        Assert.assertEquals(4, sqe.getFlagsUnion().getAccept_flags());
        Assert.assertEquals(4, sqe.getFlagsUnion().getRw_flags());
        sqe.setAddr3(5);
        Assert.assertEquals(5, sqe.getAddr3());
        Assert.assertEquals(5, sqe.getAddr3Union().getAddr3().getAddr3());
        sqe.setPad2(6);
        Assert.assertEquals(6, sqe.getPad2());
        Assert.assertEquals(6, sqe.getAddr3Union().getAddr3().get__pad2().getAtIndex(ValueLayout.JAVA_LONG, 0));


        MemoryLayout ioUringParamsLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringParams.class).withName("io_uring_params");

        Assert.assertEquals(struct.io_uring_params.layout.byteSize(), ioUringParamsLayout.byteSize());
        Assert.assertEquals(struct.io_uring_params.layout, ioUringParamsLayout);


        MemoryLayout ioUringSqLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSq.class).withName("io_uring_sq");
        Assert.assertEquals(struct.io_uring_sq.layout.byteSize(), ioUringSqLayout.byteSize());
        Assert.assertEquals(struct.io_uring_sq.layout, ioUringSqLayout);

        MemoryLayout ioUringCqLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCq.class).withName("io_uring_cq");
        Assert.assertEquals(struct.io_uring_cq.layout.byteSize(), ioUringCqLayout.byteSize());
        Assert.assertEquals(struct.io_uring_cq.layout, ioUringCqLayout);

        MemoryLayout ioUringLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUring.class).withName("io_uring");
        Assert.assertEquals(struct.io_uring.layout.byteSize(), ioUringLayout.byteSize());
        Assert.assertEquals(struct.io_uring.layout, ioUringLayout);
    }


}
