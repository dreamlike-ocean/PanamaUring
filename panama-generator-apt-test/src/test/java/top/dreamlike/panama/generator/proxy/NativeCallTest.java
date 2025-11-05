package top.dreamlike.panama.generator.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.dreamlike.panama.generator.helper.NativeStructEnhanceMark;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;

public class NativeCallTest {
    private final StructProxyGenerator structProxyGenerator;
    private final NativeCallGenerator nativeCallGenerator;

    public NativeCallTest() {
        structProxyGenerator = new StructProxyGenerator();
        nativeCallGenerator = new NativeCallGenerator(structProxyGenerator);
        nativeCallGenerator.plainMode();
    }

    @Test
    public void testGenerate() throws ClassNotFoundException {
        String s = nativeCallGenerator.generateProxyClassName(NativeLib.class);
        Class<?> aClass = Class.forName(s, false, NativeCallTest.class.getClassLoader());
        Assertions.assertNotNull(aClass);
    }

    @Test
    public void testPage() {
        NativeLib nativeLib = nativeCallGenerator.generate(NativeLib.class);
        Assertions.assertEquals(nativeLib.getPageSize(), nativeLib.page());
    }

    @Test
    public void testStruct() throws ClassNotFoundException {
        String epollEventAptName = StructProxyGenerator.generateProxyClassName(EpollEvent.class);
        String epollEventDataAptName = StructProxyGenerator.generateProxyClassName(EpollEventData.class);
        Class<?> epollEventAptClass = Class.forName(epollEventAptName, false, NativeCallTest.class.getClassLoader());
        Class<?> epollEventDataAptClass = Class.forName(epollEventDataAptName, false, NativeCallTest.class.getClassLoader());
        Assertions.assertNotNull(epollEventAptClass);
        Assertions.assertNotNull(epollEventDataAptClass);

        EpollEvent epollEvent = structProxyGenerator.allocate(Arena.ofAuto(), EpollEvent.class);
        Assertions.assertInstanceOf(NativeStructEnhanceMark.class, epollEvent);
        NativeStructEnhanceMark mark = (NativeStructEnhanceMark) epollEvent;
        MemoryLayout layout = mark.layout();
        Assertions.assertNotNull( layout);
    }
}
