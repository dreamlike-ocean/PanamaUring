package top.dreamlike.panama.generator.proxy;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.NativeFunction;

@CompileTimeGenerate
@CLib("libperson.so")
public interface NativeLib {
    @NativeFunction("getpagesize")
    int getPageSize();

    @NativeFunction("getpagesize")
    int page();
}