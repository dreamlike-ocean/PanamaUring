package top.dreamlike.panama.generator.test.struct;

import top.dreamlike.panama.generator.annotation.NativeArrayMark;
import top.dreamlike.panama.generator.annotation.Pointer;

import java.lang.foreign.MemorySegment;

public class PointerVersionTestContainer {
    int size;
    Person single;

    @Pointer
    Person ptr;

    TestContainer.UnionStruct unionStruct;

    @NativeArrayMark(size = Person.class, length = 3)
    MemorySegment personArray;

    @NativeArrayMark(size = Person.class, length = 5, asPointer = true)
    MemorySegment arrayButPointer;

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

    public TestContainer.UnionStruct getUnionStruct() {
        return unionStruct;
    }

    public void setUnionStruct(TestContainer.UnionStruct unionStruct) {
        this.unionStruct = unionStruct;
    }

    public MemorySegment getPersonArray() {
        return personArray;
    }

    public void setPersonArray(MemorySegment personArray) {
        this.personArray = personArray;
    }

    public MemorySegment getArrayButPointer() {
        return arrayButPointer;
    }

    public void setArrayButPointer(MemorySegment arrayButPointer) {
        this.arrayButPointer = arrayButPointer;
    }
}
