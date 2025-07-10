package com.xm.sanvanfo.common.plugin;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CustomPlugin {
    Class registerClass() default void.class;
    String  name() default "";
    String note() default "";
}
