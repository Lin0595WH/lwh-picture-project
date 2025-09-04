package com.lwh.pictureproject.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @Author Lin
 * @Date 2025/9/4 22:05
 * @Descriptions 处理pictureService循环依赖而加
 */
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
public class AopConfig {
}
