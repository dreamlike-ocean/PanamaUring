package top.dreamlike.panama.generator.test;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.proxy.NativeArray;
import top.dreamlike.panama.generator.proxy.NativeCallGenerator;
import top.dreamlike.panama.generator.proxy.StructProxyGenerator;
import top.dreamlike.panama.generator.test.call.LibPerson;
import top.dreamlike.panama.generator.test.struct.Person;
import top.dreamlike.panama.generator.test.struct.PointerVersionTestContainer;
import top.dreamlike.panama.generator.test.struct.SkipStruct;
import top.dreamlike.panama.generator.test.struct.TestContainer;

import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
import java.util.List;

public class PlainGeneratorTest {

    private static StructProxyGenerator structProxyGenerator;

    private static NativeCallGenerator callGenerator;

    private static LibPerson libPerson;

    @BeforeClass
    public static void init() {
        structProxyGenerator = new StructProxyGenerator();
        callGenerator = new NativeCallGenerator(structProxyGenerator);
        callGenerator.plainMode();
        libPerson = callGenerator.generate(LibPerson.class);
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
    public void testSkip() {
        MemoryLayout skipStructLayout = structProxyGenerator.extract(SkipStruct.class);
        Assert.assertEquals(ValueLayout.JAVA_INT.byteSize(), skipStructLayout.byteSize());
        Assert.assertTrue(skipStructLayout instanceof StructLayout);
        if (skipStructLayout instanceof  StructLayout structLayout) {
            List<MemoryLayout> memoryLayoutList = structLayout.memberLayouts();
            Assert.assertEquals(1, memoryLayoutList.size());
            long byteSize = memoryLayoutList.get(0).byteSize();
            Assert.assertEquals(ValueLayout.JAVA_INT.byteSize(), byteSize);
        }
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

        int newA = 1;

        //_______________写入一整块测试___________________________
        MemorySegment newChunk = Arena.global().allocate(personSizeof, 3);
        NativeArray<Person> newChunkPersonArray = new NativeArray<>(structProxyGenerator, newChunk, Person.class);
        newChunkPersonArray.forEach(p -> p.setA(newA));
        initContainer.setPersonArray(newChunkPersonArray);
        NativeArray<Person> chunkPersonArrayInStruct = initContainer.getPersonArray();
        MemorySegment chunkPersonArrayInStructMemorySegment = chunkPersonArrayInStruct.address();
        Assert.assertEquals(personArray.address().address(), chunkPersonArrayInStructMemorySegment.address());
        for (Person p : chunkPersonArrayInStruct) {
            Assert.assertEquals(p.getA(), newA);
        }

        personArray = initContainer.getArrayButPointer();
        Assert.assertEquals(5, personArray.size());
        for (Person p : personArray) {
            int a = p.getA();
            Assert.assertEquals(targetPersonA, a);
            long n = p.getN();
            Assert.assertEquals(targetPersonN, n);
        }
        //_______________写入ptr测试___________________________
        MemorySegment oldPtr = personArray.address();
        MemorySegment newPtr = Arena.global().allocate(personSizeof, 5);
        NativeArray<Person> newPersonArray = new NativeArray<>(structProxyGenerator, newPtr, Person.class);
        initContainer.setArrayButPointer(newPersonArray);
        NativeArray<Person> afterReplace = initContainer.getArrayButPointer();
        Assert.assertEquals(newPtr.address(), afterReplace.address().address());

        for (Person newPerson : afterReplace) {
            newPerson.setA(newA);
        }

        for (Person newPerson : afterReplace) {
            int a = newPerson.getA();
            Assert.assertEquals(a, newA);
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
    public void testPtrField() throws NoSuchFieldException {
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

        Person ptr = initContainer.getPtr();
        Assert.assertEquals(StructProxyGenerator.findMemorySegment(person).address(), StructProxyGenerator.findMemorySegment(ptr).address());
        Assert.assertEquals(targetPersonA, ptr.getA());


        MemorySegment newPersonInMemory = Arena.global().allocate(personSizeof);
        Person newPerson = structProxyGenerator.enhance(Person.class, newPersonInMemory);
        newPerson.setA(123);
        initContainer.setPtr(newPerson);
        Assert.assertEquals(123, initContainer.getPtr().getA());

        VarHandle varHandle = structProxyGenerator.findFieldVarHandle(PointerVersionTestContainer.class.getDeclaredField("ptr"));
        MemorySegment ptrInStruct = (MemorySegment) varHandle.get(StructProxyGenerator.findMemorySegment(initContainer));
        Assert.assertEquals(ptrInStruct.address(), newPersonInMemory.address());

        varHandle.set(StructProxyGenerator.findMemorySegment(initContainer), StructProxyGenerator.findMemorySegment(ptr));
        Assert.assertEquals(targetPersonA,initContainer.getPtr().getA());

    }

    @Test
    public void testSetPtr() {
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
        MemorySegment segment = StructProxyGenerator.findMemorySegment(initContainer);
        //等大的内存块
        PointerVersionTestContainer p = structProxyGenerator.enhance(segment);

        MemorySegment otherPerson = Arena.global().allocate(personSizeof, 5);
        p.setArrayButPointer(otherPerson);
        Assert.assertEquals(otherPerson.address(), p.getArrayButPointer().address());

    }

    @Test
    public void testVarHandle() throws NoSuchFieldException {
        MemoryLayout layout = structProxyGenerator.extract(Person.class);
        MemorySegment personCStruct = Arena.global().allocate(layout);
        Person p = structProxyGenerator.enhance(personCStruct);
        p.setA(123);
        VarHandle varHandle = structProxyGenerator.findFieldVarHandle(Person.class.getDeclaredField("a"));
        int i = (int) varHandle.get(personCStruct);
        Assert.assertEquals(123,i);
        varHandle.set(personCStruct, 456);
        Assert.assertEquals(456, p.getA());
        varHandle.toMethodHandle(VarHandle.AccessMode.GET);
    }

    @Test
    public void testDeferenceVarHandle() {
        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);

        MemoryLayout testContainerLayout = structProxyGenerator.extract(TestContainer.class);
        VarHandle nVarHandle = testContainerLayout.varHandle(
                MemoryLayout.PathElement.groupElement("ptr"),
                MemoryLayout.PathElement.dereferenceElement(),
                MemoryLayout.PathElement.groupElement("n")
        );

        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        long targetPersonN = 1L;
        int targetPersonA = 1000;
        person.setN(targetPersonN);
        person.setA(targetPersonA);
        int testContainerSize = 5;
        int testContainerUnionValue = 20;
        TestContainer initContainer = libPerson.initContainer(testContainerSize, person, testContainerUnionValue);
        MemorySegment segment = StructProxyGenerator.findMemorySegment(initContainer);

        Assert.assertEquals(targetPersonN, (long) nVarHandle.get(segment, 0));

        nVarHandle.set(segment, 0, 214);
        Assert.assertEquals(214, initContainer.getPtr().getN());

        MemoryLayout intPtrLayout = structProxyGenerator.extract(IntPtr.class);
        VarHandle intVarhandle = intPtrLayout.varHandle(
                MemoryLayout.PathElement.groupElement("ptr"),
                MemoryLayout.PathElement.dereferenceElement()
        );

        MemorySegment intPtrMemory = Arena.global().allocate(intPtrLayout);
        MemorySegment intMemory = Arena.global().allocate(ValueLayout.JAVA_INT);
        intMemory.set(ValueLayout.JAVA_INT,0, 329);
        intPtrMemory.set(ValueLayout.ADDRESS, 0, intMemory);

        int i = (int) intVarhandle.get(intPtrMemory, 0);
        Assert.assertEquals(329, i);
        intVarhandle.set(intPtrMemory, 0, 129);
        Assert.assertEquals(129, intMemory.get(ValueLayout.JAVA_INT, 0));
    }


    public static class IntPtr {
        @Pointer(targetLayout = int.class)
        private MemorySegment ptr;
    }


}
