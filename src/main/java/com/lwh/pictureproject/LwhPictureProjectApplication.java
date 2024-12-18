package com.lwh.pictureproject;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.lwh.pictureproject.mapper")
public class LwhPictureProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(LwhPictureProjectApplication.class, args);
    }

}
