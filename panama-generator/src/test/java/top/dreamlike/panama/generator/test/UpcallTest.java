package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.helper.FunctionPointer;
import top.dreamlike.panama.generator.proxy.NativeArray;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.struct.Person;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

public class UpcallTest {

    private static StructProxyGenerator structProxyGenerator = new StructProxyGenerator();

    private static NativeCallGenerator nativeCallGenerator = new NativeCallGenerator(structProxyGenerator);

    private static Method javaMethod;

    private static Method comparMethod;

    private static Method comparInstanceMethod;

    @BeforeClass
    public static void init() throws NoSuchMethodException {
        javaMethod = UpcallTest.class.getDeclaredMethod("javaMethod", int.class, int.class);
        comparMethod = UpcallTest.class.getDeclaredMethod("compar", Person.class, Person.class);
        comparInstanceMethod = UpcallTest.class.getDeclaredMethod("comparInInstance", Person.class, Person.class);
    }

    public static int compar(Person a, Person b) {
        return (int) (a.getN() - b.getN());
    }

    public static int javaMethod(int a, int b) {
        return a + b;
    }

    @Test
    public void upcallSimpleTest() throws Throwable {
        MemorySegment segment = nativeCallGenerator.generateUpcall(
                Arena.global(),
                javaMethod
        );

        MethodHandle downcallMH = Linker.nativeLinker()
                .downcallHandle(FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT))
                .bindTo(segment);
        int c = (int) downcallMH.invokeExact(1, 2);
        Assert.assertEquals(3, c);
    }

    @Test
    public void upcallObjectTest() throws Throwable {
        MemorySegment segment = nativeCallGenerator.generateUpcall(
                Arena.global(),
                comparMethod
        );
        MethodHandle downcallMH = Linker.nativeLinker()
                .downcallHandle(FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS))
                .bindTo(segment);
        MemoryLayout layout = structProxyGenerator.extract(Person.class);
        Person arg0 = structProxyGenerator.enhance(Person.class, Arena.global().allocate(layout));
        Person arg1 = structProxyGenerator.enhance(Person.class, Arena.global().allocate(layout));
        arg0.setN(2);
        arg1.setN(1);
        int i = (int) downcallMH.invokeExact(StructProxyGenerator.findMemorySegment(arg0), StructProxyGenerator.findMemorySegment(arg1));
        Assert.assertEquals(i, 1);
    }

    @Test
    public void upcallObjectWithReceiverTest() throws Throwable {
        var segment = nativeCallGenerator.generateUpcall(
                Arena.global(),
                comparInstanceMethod,
                new UpcallTest()
        );
        MethodHandle downcallMH = Linker.nativeLinker()
                .downcallHandle(FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS))
                .bindTo(segment);
        MemoryLayout layout = structProxyGenerator.extract(Person.class);
        Person arg0 = structProxyGenerator.enhance(Person.class, Arena.global().allocate(layout));
        Person arg1 = structProxyGenerator.enhance(Person.class, Arena.global().allocate(layout));
        arg0.setN(2);
        arg1.setN(1);
        int i = (int) downcallMH.invokeExact(StructProxyGenerator.findMemorySegment(arg0), StructProxyGenerator.findMemorySegment(arg1));
        Assert.assertEquals(i, 1);
    }

    @Test
    public void upcallQsortTest() throws Throwable {

        MemoryLayout layout = structProxyGenerator.extract(Person.class);
        MemorySegment nativeArrayBase = Arena.global().allocateArray(layout, 3);
        NativeArray<Person> nativeArray = structProxyGenerator.enhanceArray(nativeArrayBase);
        for (int i = 0; i < nativeArray.size(); i++) {
            // 3 2 1
            nativeArray.get(i).setN(nativeArray.size() - i);
        }

        var comparFP = nativeCallGenerator.generateUpcallFP(
                Arena.global(),
                comparMethod
        );
        Qsort qsort = nativeCallGenerator.generate(Qsort.class);
        qsort.qsort(nativeArray, nativeArray.size(), layout.byteSize(), comparFP);
        for (int i = 0; i < nativeArray.size(); i++) {
            Assert.assertEquals(i + 1, nativeArray.get(i).getN());
        }
    }

    public int comparInInstance(Person a, Person b) {
        return (int) (a.getN() - b.getN());
    }


    interface Qsort {
        void qsort(@Pointer NativeArray<Person> base, long nmemb, long size, FunctionPointer compar);
    }

}
