package top.dreamlike.panama.example;

import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;

@CompileTimeGenerate
public class Person {
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
