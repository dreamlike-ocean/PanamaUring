package top.dreamlike.panama.example;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.Pointer;


@CompileTimeGenerate
@CLib(value = "./libperson.so", inClassPath = false)
public interface StdLib {
    @NativeFunction(fast = true)
    int getpid();

    @NativeFunction(fast = true)
    int add(int a, int b);

    @NativeFunction(fast = true, returnIsPointer = true)
    Person fillPerson(int a, long n);

    @NativeFunction(fast = true, returnIsPointer = true)
    TestContainer initContainer(int size, @Pointer Person person, long unionValue);
}
