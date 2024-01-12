package top.dreamlike.panama.example;

import top.dreamlike.panama.generator.annotation.NativeFunction;

public interface StdLib {
    @NativeFunction(fast = true)
    int getpid();
}
