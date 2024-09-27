package top.dreamlike.panama.generator.test.call;

import top.dreamlike.panama.generator.annotation.CLib;
import top.dreamlike.panama.generator.annotation.NativeFunction;

//getpagesize
@CLib(prefix = "get", suffix = "size")
public interface LibPage {

    @NativeFunction("page")
    int getPageSize();

    int page();
}
