package top.dreamlike.panama.generator.test.call;

import top.dreamlike.panama.generator.test.struct.Person;
import top.dreamlike.panama.generator.test.struct.TestContainer;
import top.dreamlike.panama.genertor.annotation.CLib;
import top.dreamlike.panama.genertor.annotation.NativeFunction;
import top.dreamlike.panama.genertor.annotation.Pointer;

import java.lang.foreign.MemorySegment;

@CLib("libperson.so")
public interface LibPerson {

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

    void setSingle(@Pointer TestContainer testContainer, Person person);

    int testContainerSize();

    @NativeFunction(fast = true, returnIsPointer = true)
    MemorySegment getArrayButPointer(@Pointer TestContainer container);
}