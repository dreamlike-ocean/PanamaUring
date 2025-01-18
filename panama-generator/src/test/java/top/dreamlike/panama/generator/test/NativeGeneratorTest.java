package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import top.dreamlike.panama.generator.proxy.NativeArray;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.call.LibPerson;
import top.dreamlike.panama.generator.test.struct.Person;
import top.dreamlike.panama.generator.test.struct.PointerVersionTestContainer;
import top.dreamlike.panama.generator.test.struct.TestContainer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;

@RunWith(Parameterized.class)
public class NativeGeneratorTest {

    private final StructProxyGenerator structProxyGenerator;

    private final NativeCallGenerator callGenerator;

    private final LibPerson libPerson;

    public NativeGeneratorTest(StructProxyGenerator structProxyGenerator, NativeCallGenerator callGenerator) {
        this.structProxyGenerator = structProxyGenerator;
        this.callGenerator = callGenerator;
        libPerson = callGenerator.generate(LibPerson.class);
    }
    @Parameterized.Parameters
    public static Object[] data() {
        NativeCallGenerator indyNativeCallGenerator = new NativeCallGenerator();
        indyNativeCallGenerator.indyMode();
        StructProxyGenerator indyStructProxyGenerator = new StructProxyGenerator();

        NativeCallGenerator plainNativeCallGenerator = new NativeCallGenerator();
        plainNativeCallGenerator.plainMode();
        StructProxyGenerator plainStructProxyGenerator = new StructProxyGenerator();

        return new Object[][]{
                {indyStructProxyGenerator, indyNativeCallGenerator},
                {plainStructProxyGenerator, plainNativeCallGenerator}
        };
    }

    @Test
    public void testSimpleFunction() {
        int add = libPerson.add(1, 2);
        Assert.assertEquals(3, add);
    }

    @Test
    public void testSimpleFunctionHeap() {
        long[] longs = new long[10];
        Assert.assertEquals( 0, longs[1]);
        libPerson.set_array(longs,1, 20010329);
        Assert.assertEquals(20010329, longs[1]);
    }

    @Test
    public void testSimpleStruct() {
        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);
        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        person.setN(1);
        person.setA(2);
        Assert.assertEquals(1, libPerson.getN(person));
        Assert.assertEquals(2, libPerson.getA(person));
    }

    @Test
    public void testReturnPointerFunction() {
        Person person = libPerson.fillPerson(2, 3);
        int a = person.getA();
        Assert.assertEquals(2, a);
        long n = person.getN();
        Assert.assertEquals(3, n);

        a = libPerson.getA(person);
        n = libPerson.getN(person);
        Assert.assertEquals(2, a);
        Assert.assertEquals(3, n);

        person.setA(10);
        Assert.assertEquals(10, libPerson.getA(person));
        person.setN(100);
        Assert.assertEquals(100, libPerson.getN(person));
    }

    @Test
    public void testComplexStruct() {
        MemoryLayout testContainerLayout = structProxyGenerator.extract(TestContainer.class);
        Assert.assertEquals(libPerson.testContainerSize(), testContainerLayout.byteSize());

        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);
        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        long targetPersonN = 1L;
        int targetPersonA = 1000;
        person.setN(targetPersonN);
        person.setA(targetPersonA);

        int testContainerSize = 5;
        int testContainerUnionValue = 20;
        TestContainer initContainer = libPerson.initContainer(testContainerSize, person, testContainerUnionValue);

        Assert.assertEquals(testContainerSize, libPerson.getSize(initContainer));
        Assert.assertEquals(testContainerSize, initContainer.getSize());

        TestContainer.UnionStruct unionStruct = initContainer.getUnionStruct();

        Assert.assertEquals(testContainerUnionValue, unionStruct.getUnion_b());
        Assert.assertEquals(testContainerUnionValue, libPerson.getUnionB(initContainer));

        Assert.assertEquals(targetPersonA, initContainer.getSingle().getA());

        NativeArray<Person> personArray = initContainer.getPersonArray();
        Assert.assertEquals(3, personArray.size());
        for (Person p : personArray) {
            int a = p.getA();
            Assert.assertEquals(targetPersonA, a);
            long n = p.getN();
            Assert.assertEquals(targetPersonN, n);
        }

        personArray = initContainer.getArrayButPointer();
        Assert.assertEquals(5, personArray.size());
        for (Person p : personArray) {
            int a = p.getA();
            Assert.assertEquals(targetPersonA, a);
            long n = p.getN();
            Assert.assertEquals(targetPersonN, n);
        }
    }

    @Test
    public void testCopyFunction() {
        MemoryLayout testContainerLayout = structProxyGenerator.extract(TestContainer.class);
        Assert.assertEquals(libPerson.testContainerSize(), testContainerLayout.byteSize());

        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);
        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        long targetPersonN = 1L;
        int targetPersonA = 1000;
        person.setN(targetPersonN);
        person.setA(targetPersonA);

        MemorySegment segment = Arena.global().allocate(testContainerLayout);
        TestContainer testContainer = structProxyGenerator.enhance(TestContainer.class, segment);

        Assert.assertNotEquals(targetPersonA, testContainer.getSingle().getA());
        libPerson.setSingle(testContainer, person);
        Assert.assertEquals(targetPersonA, testContainer.getSingle().getA());

    }


    @Test
    public void testMemorySegmentVersionComplexStruct() {
        MemoryLayout layout = structProxyGenerator.extract(PointerVersionTestContainer.class);
        Assert.assertEquals(libPerson.testContainerSize(), layout.byteSize());

        MemoryLayout testContainerLayout = structProxyGenerator.extract(TestContainer.class);
        Assert.assertEquals(libPerson.testContainerSize(), testContainerLayout.byteSize());

        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);
        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        long targetPersonN = 1L;
        int targetPersonA = 1000;
        person.setN(targetPersonN);
        person.setA(targetPersonA);

        int testContainerSize = 5;
        int testContainerUnionValue = 20;
        TestContainer initContainer = libPerson.initContainer(testContainerSize, person, testContainerUnionValue);
        MemorySegment pointer = libPerson.getArrayButPointer(initContainer);
        //_______________________以上是初始化
        MemorySegment realMemory = StructProxyGenerator.findMemorySegment(initContainer);
        PointerVersionTestContainer target = structProxyGenerator.enhance(realMemory);
        MemorySegment arrayButPointer = target.getArrayButPointer();
        long address = arrayButPointer.address();
        Assert.assertEquals(personSizeof.byteSize() * 5, arrayButPointer.byteSize());
        Assert.assertEquals(pointer.address(), address);

        NativeArray<Person> personNativeArray = structProxyGenerator.enhanceArray(arrayButPointer);
        Assert.assertEquals(5, personNativeArray.size());
        for (Person p : personNativeArray) {
            int a = p.getA();
            Assert.assertEquals(targetPersonA, a);
            long n = p.getN();
            Assert.assertEquals(targetPersonN, n);
        }

        MemorySegment personArray = target.getPersonArray();

        personNativeArray = structProxyGenerator.enhanceArray(personArray);
        Assert.assertEquals(3, personNativeArray.size());
        for (Person p : personNativeArray) {
            int a = p.getA();
            Assert.assertEquals(targetPersonA, a);
            long n = p.getN();
            Assert.assertEquals(targetPersonN, n);
        }
    }

    @Test
    public void testFp() {
        MemorySegment return1Fp = SymbolLookup.loaderLookup()
                .find("return1").get();
        MemorySegment rawAddFp = SymbolLookup.loaderLookup()
                .find("raw_add").get();
       Assert.assertEquals(1,  libPerson.return1(return1Fp));
        Assert.assertEquals(3,  libPerson.rawAdd(rawAddFp, 1, 2));
    }


}
