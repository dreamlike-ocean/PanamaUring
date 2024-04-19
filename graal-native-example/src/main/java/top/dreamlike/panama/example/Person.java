package top.dreamlike.panama.example;

import top.dreamlike.panama.generator.annotation.CompileTimeGenerate;
import top.dreamlike.panama.generator.annotation.ShortcutOption;

import java.lang.invoke.VarHandle;

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
        return "a: " + getA() + ", n: " + getN();
    }

    @CompileTimeGenerate(CompileTimeGenerate.GenerateType.SHORTCUT)
    public interface PersonShort {
        @ShortcutOption(value = "a", owner = Person.class, mode = VarHandle.AccessMode.SET)
        void setPersonA(Person person, int a);

        @ShortcutOption(value = "a", owner = Person.class, mode = VarHandle.AccessMode.GET)
        int getPersonA(Person person);

    }
}
