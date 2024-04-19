package top.dreamlike.panama.generator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.VarHandle;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ShortcutOption {
    String[] value();

    VarHandle.AccessMode mode() default VarHandle.AccessMode.GET;

    Class owner();
}
