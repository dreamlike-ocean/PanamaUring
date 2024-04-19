package top.dreamlike;


import top.dreamlike.panama.example.Person;
import top.dreamlike.panama.example.StdLib;
import top.dreamlike.panama.example.TestContainer;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

public class Main {

    private static final StdLib STD_LIB;

    private static final Person.PersonShort personShortcut;

    static {
        StructProxyGenerator proxyGenerator = new StructProxyGenerator();
        NativeCallGenerator generator = new NativeCallGenerator(proxyGenerator);
        STD_LIB = generator.generate(StdLib.class);
        proxyGenerator.register(Person.class);
        proxyGenerator.register(TestContainer.class);
        personShortcut = proxyGenerator.generateShortcut(Person.PersonShort.class).get();
    }

    static {
        System.load("/usr/lib/x86_64-linux-gnu/libc.so.6");
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("in native?: " + System.getProperty("org.graalvm.nativeimage.kind"));
        int pid = STD_LIB.getpid();
        System.out.println("pid: " + pid);
        int add = STD_LIB.add(10, 10);
        System.out.println("add :" + add);
        Person person = STD_LIB.fillPerson(1, 100L);
        System.out.println(person);

        System.out.println(personShortcut.getPersonA(person));
    }

}