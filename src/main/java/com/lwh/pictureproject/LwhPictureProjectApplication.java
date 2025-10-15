package com.lwh.pictureproject;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.lwh.pictureproject.mapper")
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
public class LwhPictureProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(LwhPictureProjectApplication.class, args);
    }

}
