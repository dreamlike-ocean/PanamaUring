# Panama Generator

这是一个提供声明式的Panama FFI API绑定的运行时库，Maven坐标为：

```xml

<dependency>
    <groupId>io.github.dreamlike-ocean</groupId>
    <artifactId>panama-generator</artifactId>
    <version>1.2</version>
</dependency>
```

## 基础组件

基础组件分为两个

- StructProxyGenerator 用来从Java类的声明生成对应Struct的绑定，生成对应MemorySegment的各种get和set操作
- NativeCallGenerator 用于从Java接口声明生成对应的native call的绑定。

## StructProxyGenerator

### 基础使用

首先你先来个java类的声明，比如像这样：

```java
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
}

```

然后这样绑定一下 就好了

```java
        MemoryLayout personSizeof = structProxyGenerator.extract(Person.class);
        MemorySegment personInMemory = Arena.global().allocate(personSizeof);
        Person person = structProxyGenerator.enhance(Person.class, personInMemory);
        person.setN(1);
        person.setA(2);
```

对于原始类型的字段全部的setter和getter都会被劫持成对绑定的那块MemorySegment的操作

然后我们看一个复杂一点的例子,包含Union和包含结构体，甚至还有指针之类的

```c
   struct Person {
        int a;
        long n;
    };

    struct TestContainer {
        int size;
        Person single;
        union {
            int union_a;
            long union_b;
        };
        Person personArray[3];
        //
        Person* arrayButPointer;
    };
```

那么对应的java类应该怎么写呢？


```java
public class TestContainer {
    int size;
    Person single;

    UnionStruct unionStruct;

    @NativeArrayMark(size = Person.class, length = 3)
    NativeArray<Person> personArray;

    @NativeArrayMark(size = Person.class, length = 5, asPointer = true)
    NativeArray<Person> arrayButPointer;

    @Skip
    String c;

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

    public UnionStruct getUnionStruct() {
        return unionStruct;
    }

    public void setUnionStruct(UnionStruct unionStruct) {
        this.unionStruct = unionStruct;
    }

    public NativeArray<Person> getPersonArray() {
        return personArray;
    }

    public void setPersonArray(NativeArray<Person> personArray) {
        this.personArray = personArray;
    }

    public NativeArray<Person> getArrayButPointer() {
        return arrayButPointer;
    }

    public void setArrayButPointer(NativeArray<Person> arrayButPointer) {
        this.arrayButPointer = arrayButPointer;
    }

    @Union
    public static class UnionStruct {
        int union_a;
        long union_b;

        public int getUnion_a() {
            return union_a;
        }

        public void setUnion_a(int union_a) {
            this.union_a = union_a;
        }

        public long getUnion_b() {
            return union_b;
        }

        public void setUnion_b(long union_b) {
            this.union_b = union_b;
        }
    }
}
```
> @Skip表示不生成这个字段的绑定
就这么简单，这里涉及到指针的我得多说几句

- NativeArrayMark这个一定要标识是啥类类型，多长要不NativeArray没法正确实例化
- 如果表现为一个指针但是实际上是个数组，记得asPointer这个参数
- 不使用NativeArray也可以，直接使用MemorySegment也是支持的，只要你的NativeArrayMark标注的是对的

### shortcut

某些时候你可能不想那么OOP，只想从某个MemorySegment或者某个被增强的对象里面取到一个嵌套的值，或者使用某个内存顺序，那么就可以用这种方式

- 返回值必须要是对应的原始类型，
- 如果第一个参数为MemorySegment那么owner必填
- value为取值的路径，这里就是EpollEventGenerated.data.u64
- mode为你需要的类型
```java
@ShortcutOption(value = {"data", "u64"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.GET)
long getU64(EpollEventGenerated eventGenerated);

@ShortcutOption(value = {"data", "u64"}, owner = EpollEventGenerated.class, mode = VarHandle.AccessMode.GET)
long getU64(MemorySegment eventGenerated);
```

### 后门

- 通过StructProxyGenerator::isNativeStruct 来判断一个实例是不是绑定到一个MemorySegment上
- 通过StructProxyGenerator::findMemorySegment 来获取一个实例绑定的MemorySegment
- 通过StructProxyGenerator::rebind 来让一个实例重新绑定MemorySegment

## NativeCallGenerator

### 基础使用

对于这样一个头文件

```cpp
int add(int a, int b);
struct Person* fillPerson(int a, long n);
int getA(Person* person);
int current_error(int dummy, long dummy2);
```

你需要这样声明下

```java

@CLib("libperson.so")
public interface LibPerson {

    @NativeFunction(fast = true)
    int add(int a, int b);

    @NativeFunction(fast = true, returnIsPointer = true)
    Person fillPerson(int a, long n);

    int getA(@Pointer Person person);

    @NativeFunction(fast = true, needErrorNo = true)
    int current_error(int dummy, long dummy2);
}    
```

使用为

```java
var structProxyGenerator = new StructProxyGenerator();
var callGenerator = new NativeCallGenerator(structProxyGenerator);
var libPerson = callGenerator.generate(LibPerson.class);
```

让我们简单回答下这些注解的含义

- @CLib 这里是填充需要读取的动态库名，默认在类路径下，如果inClassPath为false则默认使用绝对路径加载
- @NativeFunction
    - value 对应native函数名若为空则默认使用被注解的函数的名字
    - fast 是否使用Linker.Option.isTrivial()这个链接参数，注意对于调用时间长的函数会对JVM产生很恶劣的性能影响
    - allowPassHeap jdk22后支持用heap的MemorySegment传递到native，如果接口签名中存在某个参数是原始类型数组则会自动使用allowPassHeap
    - returnIsPointer 如果native函数返回了一个struct的指针，通过声明这个属性为true可以将函数返回值自动映射为对应的这里支持使用对应被StructProxyGenerator增强过的Java类
    - needErrorNo，如果设置为true则会使用Linker.Option.captureCallState("errno"),具体后面会讲
- @Pointer 如果native入参是一个结构体指针，这里支持使用对应被StructProxyGenerator增强过的Java类作为入参

剩余的注意事项是

- 当native函数返回空指针时，会返回null
- 当native函数返回非空指针时，会自动将返回值映射为对应被StructProxyGenerator增强过的Java类
- 每一个非原始对象入参都必须要先被StructProxyGenerator增强过，否则会抛出异常

### errorno

Java Panama FFI errorno api其实是有点奇怪的，所以你需要这样使用

```java
    public void testError() {
    try (Arena arena = Arena.ofConfined()) {
        MemoryLifetimeScope.of(arena)
                .active(() -> {
                    long l = libPerson.set_error_no(888, 1);
                    Assert.assertEquals(l, 1);
                    int error = libPerson.current_error(1, 2);
                    Assert.assertEquals(error, 888);
                    Assert.assertEquals(ErrorNo.error.get().intValue(), 888);
                });
    }
}
```

### function ptr

某些情况下可能只有一组方法签名一致但是入口地址不同的native函数，为了避免重复工作，所以你可以声明第一个参数是一个函数指针,注意必须要是第一个且类型为MemorySegment，此时函数名则不参与解析

```java
 int rawAdd(@NativeFunctionPointer MemorySegment fp, int a, int b);
```

### indy模式

indy就是invokeDynamic这个字节码

你可以使用如下代码切换下一次生成的绑定使用传统模式还是indy模式，同一个接口两种模式生成的结果是独立的

```
        callGenerator.indyMode();
        callGenerator.plainMode();
```

对于

```c 
int add1()
int add2()
```

#### 传统模式

生成出来类似于

```java
class Plain {
    static final Methodhandle add1MH = bind1();
    static final Methodhandle add2MH = bind2();

    int add1() {
        return add1MH.invokeExact();
    }

    int add2() {
        return add2MH.invokeExact();
    }
}

```

在类初始化的时候生成全部绑定的函数，对于native-image会友好一些

#### indy 模式

生成出来类似于

```java
class Indy {
    int add1() {
        invokedyamic bind1 ();
    }

    int add2() {
        invokedyamic bind2 ();
    }
}

```

对于每一个native调用的函数，会将绑定延迟到第一次调用时，不调用就不会产生绑定的开销