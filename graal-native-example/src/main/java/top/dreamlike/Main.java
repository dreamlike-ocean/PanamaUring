package top.dreamlike;

import top.dreamlike.panama.example.Person;
import top.dreamlike.panama.example.StdLib;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static final MethodHandle getpid$Mh;
    private static final StdLib STD_LIB;

    static {
        StructProxyGenerator proxyGenerator = new StructProxyGenerator();
        proxyGenerator.setProxySavePath("generator");
        NativeCallGenerator generator = new NativeCallGenerator(proxyGenerator);
        STD_LIB = generator.generate(StdLib.class);
    }

    static {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        System.load("/usr/lib/x86_64-linux-gnu/libc.so.6");
        MemorySegment address = SymbolLookup.loaderLookup()
                .find("getpid").get();
        getpid$Mh = Linker.nativeLinker()
                .downcallHandle(address, FunctionDescriptor.of(ValueLayout.JAVA_INT), Linker.Option.isTrivial());
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