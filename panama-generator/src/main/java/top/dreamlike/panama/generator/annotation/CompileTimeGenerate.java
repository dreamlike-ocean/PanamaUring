package top.dreamlike.panama.generator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface CompileTimeGenerate {

    /**
     * 当前需要生成什么类型的代码
     * 如果是STRUCT_PROXY，则生成结构体代理
     * 如果打标在接口是上且是SHORTCUT，则生成快捷方法
     * 如果打标在接口是上且是是NATIVE_CALL，则生成native方法调用
     *
     * @return
     */
    GenerateType value() default GenerateType.NATIVE_CALL;

    public enum GenerateType {
        STRUCT_PROXY,
        SHORTCUT,
        NATIVE_CALL
    }
}
