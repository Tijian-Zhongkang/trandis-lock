package com.xm.sanvanfo.common.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface PropertyDateFormat {
    String pattern() default "yyyy-MM-dd HH:mm:ss";
    String zone() default "Asia/Shanghai";
    String localeLangTag() default "en-US";
}
