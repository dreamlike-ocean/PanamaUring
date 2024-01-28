package top.dreamlike;

import net.bytebuddy.dynamic.scaffold.TypeWriter;
import top.dreamlike.panama.example.Person;
import top.dreamlike.panama.example.StdLib;
import top.dreamlike.panama.example.TestContainer;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

public class Main {

    private static final StdLib STD_LIB;

    static {
        String dumpFolder = System.getProperty(TypeWriter.DUMP_PROPERTY);
        System.out.println(dumpFolder);
        StructProxyGenerator proxyGenerator = new StructProxyGenerator();
        NativeCallGenerator generator = new NativeCallGenerator(proxyGenerator);
        STD_LIB = generator.generate(StdLib.class);
        proxyGenerator.register(Person.class);
        proxyGenerator.register(TestContainer.class);
    }

    static {
        System.load("/usr/lib/x86_64-linux-gnu/libc.so.6");
//        MemorySegment address = SymbolLookup.loaderLookup()
//                .find("getpid").get();
//        getpid$Mh = Linker.nativeLinker()
//                .downcallHandle(address, FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
    }

    public static void main(String[] args) throws Throwable {
        System.out.println(STR."in native?: \{System.getProperty("org.graalvm.nativeimage.kind")}");
        int pid = STD_LIB.getpid();
        System.out.println(STR."pid: \{pid}");
        int add = STD_LIB.add(10, 10);
        System.out.println(STR."add :\{add}");
        Person person = STD_LIB.fillPerson(1, 100L);
        System.out.println(person);

    }

}