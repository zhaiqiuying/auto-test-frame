package com.example.qa.btest.engine.dataProvider.mysql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Mysql {
    String driverClassName() default "com.mysql.cj.jdbc.Driver";
    String url() default "";
    String user() default "";
    String password() default "";
    String sql() default "";
}
