package com.xm.sanvanfo.common.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface PropertiesItem {
    String name() default "";
    boolean ignore() default false;
}
