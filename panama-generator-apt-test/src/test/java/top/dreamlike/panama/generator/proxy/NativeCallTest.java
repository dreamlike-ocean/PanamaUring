package top.dreamlike.panama.generator.proxy;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class NativeCallTest {

    @Test
    public void testGenerate() throws ClassNotFoundException {
        NativeCallGenerator nativeCallGenerator = new NativeCallGenerator();
        String s = nativeCallGenerator.generateProxyClassName(LibPerson.class);
        Class<?> aClass = Class.forName(s);
        Assert.assertNotNull(aClass);
    }
}
