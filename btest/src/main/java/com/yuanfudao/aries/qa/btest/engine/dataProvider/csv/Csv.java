package com.yuanfudao.aries.qa.btest.engine.dataProvider.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Csv {

    boolean isRelativePath() default true; // relativePath会默认附加环境类型,例如 test,product
    String value() default "";

    String encoding() default "";
}
