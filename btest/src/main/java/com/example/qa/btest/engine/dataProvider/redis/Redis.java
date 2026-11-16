package com.example.qa.btest.engine.dataProvider.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Redis {
    String host() default "";
    int port() default 6379;
    String password() default "";
    int database() default 0;
    String command() default "";
    String key() default "";
    String field() default "";
    String value() default "";
    String url() default "";
}
