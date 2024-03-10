import org.junit.Assert;
import org.junit.Test;
import struct.io_uring_cqe_struct;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.nativelib.Instance;
import top.dreamlike.panama.nativelib.struct.IoUringCqe;
import top.dreamlike.panama.nativelib.struct.IoUringSqe;

import java.lang.foreign.MemoryLayout;

public class LiburingTest {


    @Test
    public void testLayout() {
        MemoryLayout sqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class).withName("io_uring_sqe");
        Assert.assertEquals(io_uring_sqe_struct.layout.byteSize(), sqeLayout.byteSize());
        Assert.assertEquals(io_uring_sqe_struct.layout, sqeLayout);

        MemoryLayout cqeLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringCqe.class).withName("io_uring_cqe");
        Assert.assertEquals(io_uring_cqe_struct.layout.byteSize(), cqeLayout.byteSize());
        Assert.assertEquals(io_uring_cqe_struct.layout, cqeLayout);

    }


}
