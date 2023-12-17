import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.dreamlike.panama.genertor.annotation.*;
import top.dreamlike.panama.genertor.proxy.NativeArray;
import top.dreamlike.panama.genertor.proxy.NativeCallGenerator;
import top.dreamlike.panama.genertor.proxy.StructProxyGenerator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

public class GeneratorTest {
    //    extern "C" {
//    int add(int a, int b);
//    int add1(int a,int b);
//    struct Person {
//        int a;
//        long n;
//    };
//
//    struct Person* fillPerson(int a, long n);
//};
    private static StructProxyGenerator structProxyGenerator;

    private static NativeCallGenerator callGenerator;

    private static LibPerson libPerson;

    @BeforeClass
    public static void init() {
        structProxyGenerator = new StructProxyGenerator();
        callGenerator = new NativeCallGenerator(structProxyGenerator);
        libPerson = callGenerator.generate(LibPerson.class);
    }

    @Test
    public void testSimpleFunction() {
        int add = libPerson.add(1, 2);
        Assert.assertEquals(3, add);
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

    @CLib("libperson.so")
    interface LibPerson {

        @NativeFunction(fast = true)
        int add(int a, int b);

        @NativeFunction(fast = true, returnIsPointer = true)
        Person fillPerson(int a, long n);

        int getA(@Pointer Person person);

        long getN(@Pointer Person person);

        @NativeFunction(fast = true, returnIsPointer = true)
        TestContainer initContainer(int size, @Pointer Person person, long unionValue);

        int getSize(@Pointer TestContainer container);

        long getUnionB(@Pointer TestContainer container);


        int testContainerSize();
    }

    static class Person {
        int a;
        long n;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public long getN() {
            return n;
        }

        public void setN(long n) {
            this.n = n;
        }

        @Override
        public String toString() {
            return STR."a: \{getA()}, n: \{getN()}";
        }
    }

    static class TestContainer {
        int size;
        Person single;

        UnionStruct unionStruct;

        @NativeArrayMark(size = Person.class, length = 3)
        NativeArray<Person> personArray;

        @NativeArrayMark(size = Person.class, length = 5, asPointer = true)
        NativeArray<Person> arrayButPointer;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public Person getSingle() {
            return single;
        }

        public void setSingle(Person single) {
            this.single = single;
        }

        public UnionStruct getUnionStruct() {
            return unionStruct;
        }

        public void setUnionStruct(UnionStruct unionStruct) {
            this.unionStruct = unionStruct;
        }

        public NativeArray<Person> getPersonArray() {
            return personArray;
        }

        public void setPersonArray(NativeArray<Person> personArray) {
            this.personArray = personArray;
        }

        public NativeArray<Person> getArrayButPointer() {
            return arrayButPointer;
        }

        public void setArrayButPointer(NativeArray<Person> arrayButPointer) {
            this.arrayButPointer = arrayButPointer;
        }

        @Union
        static class UnionStruct {
            int union_a;
            long union_b;

            public int getUnion_a() {
                return union_a;
            }

            public void setUnion_a(int union_a) {
                this.union_a = union_a;
            }

            public long getUnion_b() {
                return union_b;
            }

            public void setUnion_b(long union_b) {
                this.union_b = union_b;
            }
        }
    }
}
