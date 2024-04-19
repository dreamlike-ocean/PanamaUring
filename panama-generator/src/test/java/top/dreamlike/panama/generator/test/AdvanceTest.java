package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.generator.annotation.ShortcutOption;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.struct.EpollEventGenerated;
import top.dreamlike.panama.generator.test.struct.Person;
import top.dreamlike.panama.generator.test.struct.TestContainer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

public class AdvanceTest {
    private static StructProxyGenerator structProxyGenerator;

    private static Shortcut shortcut;

    @BeforeClass
    public static void init() {
        structProxyGenerator = new StructProxyGenerator();
        shortcut = structProxyGenerator.generateShortcut(Shortcut.class).get();
    }

    @Test
    public void testPlain() {
        Person person = structProxyGenerator.allocate(Arena.global(), Person.class);
        shortcut.setPersonA(person, 123);
        Assert.assertEquals(person.getA(), 123);
        Assert.assertEquals(123, shortcut.setPersonA(person));
    }

    @Test
    public void testSubStruct() {
        EpollEventGenerated event = structProxyGenerator.allocate(Arena.global(), EpollEventGenerated.class);
        EpollEventGenerated.epoll_data_t dataT = structProxyGenerator.allocate(Arena.global(), EpollEventGenerated.epoll_data_t.class);
        dataT.setU64(123);
        event.setData(dataT);
        Assert.assertEquals(123, event.getData().getU64());

        Assert.assertEquals(123, shortcut.getU64(event));
        Assert.assertEquals(123, shortcut.getPtr(event));

        shortcut.setU64(event, 456);
        Assert.assertEquals(456, event.getData().getU64());
        Assert.assertEquals(456, shortcut.getU64(event));
        Assert.assertEquals(456, shortcut.getPtr(event));

        Assert.assertEquals(456, shortcut.getU64(StructProxyGenerator.findMemorySegment(event)));

    }

    @Test
    public void testPtr() {
        TestContainer container = structProxyGenerator.allocate(Arena.global(), TestContainer.class);
        Assert.assertNull(container.getPtr());

        Person person = structProxyGenerator.allocate(Arena.global(), Person.class);
        container.setPtr(person);
        Assert.assertNotNull(container.getPtr());

        person.setA(123);
        Assert.assertEquals(123, shortcut.getPersonA(container));
        shortcut.setPersonA(container, 456);
        Assert.assertEquals(456, person.getA());
        Assert.assertEquals(456, shortcut.getPersonA(container));
    }


    interface Shortcut {
        @ShortcutOption(value = "a", owner = Person.class, mode = VarHandle.AccessMode.SET)
        void setPersonA(Person person, int a);

        @ShortcutOption(value = "a", owner = Person.class, mode = VarHandle.AccessMode.GET)
        int setPersonA(Person person);

        @ShortcutOption(value = {"data", "u64"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.GET)
        long getU64(EpollEventGenerated eventGenerated);

        @ShortcutOption(value = {"data", "u64"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.GET)
        long getU64(MemorySegment eventGenerated);

        @ShortcutOption(value = {"data", "ptr"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.GET)
        long getPtr(EpollEventGenerated eventGenerated);

        @ShortcutOption(value = {"data", "u64"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.SET)
        void setU64(EpollEventGenerated eventGenerated, long u64);

        @ShortcutOption(value = {"ptr", "a"}, owner = TestContainer.class, mode = VarHandle.AccessMode.SET)
        void setPersonA(TestContainer container, int a);

        @ShortcutOption(value = {"ptr", "a"}, owner = TestContainer.class, mode = VarHandle.AccessMode.GET)
        int getPersonA(TestContainer container);
    }
}
