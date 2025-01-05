import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.libs.LibSysCall;

import java.lang.foreign.MemorySegment;

public class SyscallTest {

    @Test
    public void testPointer() {
        MemorySegment syscallFp = LibSysCall.SYSCALL_FP;
        Assert.assertNotNull(syscallFp);
        Assert.assertTrue(syscallFp.address() != 0);
    }
}
