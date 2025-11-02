package top.dreamlike.panama.generator.test.call;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.NativeFunctionPointer;
import top.dreamlike.panama.generator.annotation.Pointer;
import top.dreamlike.panama.generator.proxy.ErrorNo;
import top.dreamlike.panama.generator.test.struct.Person;
import top.dreamlike.panama.generator.test.struct.TestContainer;

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

    @NativeFunction(fast = false, needErrorNo = true)
    int current_error(int dummy, long dummy2);

    @NativeFunction(needErrorNo = true, errorNoType = ErrorNo.ErrorNoType.POSIX_ERROR_NO)
    long set_error_no(int error, long returnValue);

    long set_array(long[] error, int index, long value);

    int return1(@NativeFunctionPointer MemorySegment fp);

    int rawAdd(@NativeFunctionPointer MemorySegment fp, int a, int b);

    String returnCStr();

    String returnStr(String str);

    @NativeFunction(fast = true)
    Person returnStruct(int a, long n);

    @NativeFunction(needErrorNo = true, fast = true, allowPassHeap = true)
    Person returnStructAndErrorNo(int a, long n, int errorNo);
}