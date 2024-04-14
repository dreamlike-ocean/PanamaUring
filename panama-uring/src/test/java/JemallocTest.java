import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.libs.LibJemalloc;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class JemallocTest {


    @Test
    public void testMalloc() {
        LibJemalloc jemalloc = Instance.LIB_JEMALLOC;
        MemorySegment segment = jemalloc.malloc(1024);
        Assert.assertNotEquals(0,segment.address());
        segment.set(ValueLayout.JAVA_INT, 0, 214);
        Assert.assertEquals(214, segment.get(ValueLayout.JAVA_INT, 0));
        Assert.assertTrue(jemalloc.malloc_usable_size(segment) >= 1024L);
        jemalloc.free(segment);
    }
}
