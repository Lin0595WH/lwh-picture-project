package com.lwh.pictureproject.manager.captcha.model;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author Lin
 * @Date 2025/10/10 20:51
 * @Descriptions 获取邮件配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    /**
     * 邮件发送者
     */
    private String username;

    /**
     * 邮件密码
     */
    private String password;

    /**
     * 邮件服务器
     */
    private String host;

    /**
     * 邮件端口
     */
    private Long port;
}
