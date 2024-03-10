import org.junit.Assert;
import org.junit.Test;
import struct.io_uring_sqe_struct;
import top.dreamlike.panama.nativelib.Instance;
import top.dreamlike.panama.nativelib.Libc;
import top.dreamlike.panama.nativelib.struct.IoUringSqe;

import java.lang.foreign.MemoryLayout;

public class LiburingTest {



    @Test
    public void testLayout() {
        MemoryLayout memoryLayout = Instance.STRUCT_PROXY_GENERATOR.extract(IoUringSqe.class).withName("io_uring_sqe");
        Assert.assertEquals(io_uring_sqe_struct.const$1.byteSize(),memoryLayout.byteSize());
        Assert.assertEquals(io_uring_sqe_struct.const$1, memoryLayout);
    }
}
