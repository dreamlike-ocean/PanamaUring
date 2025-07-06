import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.dreamlike.panama.uring.helper.unsafe.TrustedLookup;

public class UnsafeTest {

    @Test
    public void testUnsafe() {
        Assertions.assertTrue(TrustedLookup.unsafe().isPresent());
    }

    @Test
    public void testReflectionFactory() {
        Assertions.assertTrue(TrustedLookup.reflectionFactory().isPresent());
    }

    @Test
    public void testPanama() {
        Assertions.assertTrue(TrustedLookup.panama().isPresent());
    }
}
