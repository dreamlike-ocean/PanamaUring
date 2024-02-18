package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.generator.proxy.ErrorNo;
import top.dreamlike.panama.generator.proxy.MemoryLifetimeScope;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.call.LibPerson;

import java.lang.foreign.Arena;

public class ErrorNoTest {
    private static StructProxyGenerator structProxyGenerator;

    private static NativeCallGenerator callGenerator;

    private static LibPerson libPerson;

    @BeforeClass
    public static void init() {
        structProxyGenerator = new StructProxyGenerator();
        callGenerator = new NativeCallGenerator(structProxyGenerator);
        callGenerator.indyMode();
        libPerson = callGenerator.generate(LibPerson.class);
    }

    @Test
    public void testError() {
        Assert.assertThrows(IllegalStateException.class, () -> {
            libPerson.current_error(123, 123);
        });
        try (Arena arena = Arena.ofConfined()) {
            MemoryLifetimeScope.of(arena)
                    .active(() -> {
                        long l = libPerson.set_error_no(888, 1);
                        Assert.assertEquals(l, 1);
                        int error = libPerson.current_error(1, 2);
                        Assert.assertEquals(error, 888);
                        Assert.assertEquals(ErrorNo.error.get().intValue(), 888);
                    });
        }
    }
}
