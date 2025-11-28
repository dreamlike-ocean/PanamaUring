package top.dreamlike.panama.generator.annotation;

import top.dreamlike.panama.generator.proxy.ErrorNo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NativeFunction {
    String value() default "";

    boolean fast() default false;

    boolean allowPassHeap() default false;

    boolean returnIsPointer() default false;

    boolean needErrorNo() default false;

    ErrorNo.ErrorNoType[] errorNoType() default {ErrorNo.ErrorNoType.AUTO};
}
