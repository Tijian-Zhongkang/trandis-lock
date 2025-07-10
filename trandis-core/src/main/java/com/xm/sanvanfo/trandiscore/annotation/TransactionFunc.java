package com.xm.sanvanfo.trandiscore.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TransactionFunc {

    String spaceName();
    String zookeeperLockKeysFunction() default "";
    String zookeeperReadLockKeysFunction() default "";
    String prepareFunction() default "";
    String setVersion() default "";
    String doAfterFunction() default "";
    String undoFunction() default "";
    String zookeeperAfterLockKeysFunction() default "";
    String zookeeperAfterReadLockKeysFunction() default "";
    String releaseVersion() default "";
    boolean write() default false;
    boolean lockUpgrade() default false;
}
