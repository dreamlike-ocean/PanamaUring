package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.NativeFunction;
import top.dreamlike.panama.generator.annotation.NativeFunctionPointer;

import java.lang.foreign.MemorySegment;

@CompileTimeGenerate
@CLib("libperson.so")
public interface NativeLib {
    @NativeFunction("getpagesize")
    int getPageSize();

    @NativeFunction("getpagesize")
    int page();

    @NativeFunction(value = "getpagesize", fast = true)
    int fastPage();

    @NativeFunction(fast = true, returnIsPointer = true)
    Person fillPerson(int a, long n);

    int rawAdd(@NativeFunctionPointer MemorySegment fp, int a, int b);
}