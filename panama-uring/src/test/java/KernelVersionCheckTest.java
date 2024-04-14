import org.junit.Assert;
import org.junit.Test;
import top.dreamlike.panama.uring.nativelib.Instance;
import top.dreamlike.panama.uring.nativelib.exception.ErrorKernelVersionException;
import top.dreamlike.panama.uring.nativelib.helper.KernelVersionLimit;
import top.dreamlike.panama.uring.nativelib.helper.NativeHelper;

public class KernelVersionCheckTest {

    @Test
    public void test() {
        SomeFunctionalInterface generated = Instance.NATIVE_CALL_GENERATOR.generate(SomeFunctionalInterface.class);
        SomeFunctionalInterface checked = NativeHelper.enhanceCheck(generated, SomeFunctionalInterface.class);
        Assert.assertThrows(ErrorKernelVersionException.class, checked::mustThrow);
        Assert.assertTrue(checked.getpagesize() > 0);

        Assert.assertEquals(3, checked.defaultFunction(1, 2));
        Assert.assertThrows(ErrorKernelVersionException.class, () -> checked.defaultFunctionMustThrow(1, 2));
    }

    public interface SomeFunctionalInterface {
        @KernelVersionLimit(major = 10, minor = 10)
        void mustThrow();

        @KernelVersionLimit(major = 1, minor = 1)
        int getpagesize();

        @KernelVersionLimit(major = 1, minor = 1)
        default int defaultFunction(int a, int b) {
            return a + b;
        }

        @KernelVersionLimit(major = 10, minor = 1)
        default int defaultFunctionMustThrow(int a, int b) {
            return a + b;
        }
    }
}
