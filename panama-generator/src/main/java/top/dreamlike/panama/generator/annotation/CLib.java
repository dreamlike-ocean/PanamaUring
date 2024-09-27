package top.dreamlike.panama.generator.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CLib {
    String value() default "";

    boolean inClassPath() default true;

    boolean isLib() default false;

    String prefix() default "";

    String suffix() default "";
}
