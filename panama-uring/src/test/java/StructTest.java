import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.nativelib.Instance;
import top.dreamlike.panama.nativelib.struct.sigset.SigsetType;
import top.dreamlike.panama.nativelib.struct.time.KernelTime64Type;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.JAVA_LONG;

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
}
