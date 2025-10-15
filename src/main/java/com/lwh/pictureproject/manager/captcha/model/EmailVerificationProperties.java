package com.lwh.pictureproject.manager.captcha.model;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author Lin
 * @Date 2025/10/10 20:55
 * @Descriptions 获取邮件自定义配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "email.verification")
public class EmailVerificationProperties {
    /**
     * 验证码过期时间（分钟）
     */
    private int codeExpireMinutes;

    /**
     * 验证码长度
     */
    private int codeLength;

    /**
     * 每小时最大发送次数
     */
    private int maxAttemptsPerHour;
}
