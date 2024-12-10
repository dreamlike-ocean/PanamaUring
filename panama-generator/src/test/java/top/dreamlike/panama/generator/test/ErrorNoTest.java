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
        long res = libPerson.set_error_no(999, 2);
        Assert.assertEquals(res, 2);
        Assert.assertEquals(ErrorNo.error.get().intValue(), 999);

        try (Arena arena = Arena.ofConfined()) {
            MemoryLifetimeScope.of(arena)
                    .active(() -> {
                        long res1 = libPerson.set_error_no(888, 2);
                        Assert.assertEquals(res1, 2);
                        Assert.assertEquals(ErrorNo.error.get().intValue(), 888);
                    });
        }
    }
}
