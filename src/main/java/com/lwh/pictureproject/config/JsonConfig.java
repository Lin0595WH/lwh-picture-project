package com.lwh.pictureproject.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;

/**
 * @author Lin
 * @version 1.0.0
 * @description Spring MVC Json 配置
 * @date 2024/12/19 22:14
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     * 新增全局处理时间格式化问题
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 创建一个ObjectMapper实例，用于JSON与XML的序列化与反序列化
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 设置全局日期格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 注册JavaTimeModule以支持Java 8日期时间API
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用写入空值属性
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 创建一个新的模块，用于自定义序列化器
        SimpleModule module = new SimpleModule();
        // 为Long类型添加序列化器，将其转换为字符串处理
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 将自定义模块注册到ObjectMapper中
        objectMapper.registerModule(module);
        // 返回配置好的ObjectMapper实例
        return objectMapper;
    }
}
