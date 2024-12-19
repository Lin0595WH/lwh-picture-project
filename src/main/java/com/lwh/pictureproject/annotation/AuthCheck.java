package com.lwh.pictureproject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Lin
 * @version 1.0.0
 * @description 用于校验用户权限
 * @date 2024/12/19 20:26
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 必须具有某个角色
     **/
    String mustRole() default "";
}





