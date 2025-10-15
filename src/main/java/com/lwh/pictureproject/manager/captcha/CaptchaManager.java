package com.lwh.pictureproject.manager.captcha;


import cn.hutool.core.util.RandomUtil;
import com.lwh.pictureproject.manager.captcha.model.EmailVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @Author Lin
 * @Date 2025/10/10 20:41
 * @Descriptions 验证码
 */
@Component
@RequiredArgsConstructor
public class CaptchaManager {

    private static final String VERIFICATION_CODE_PREFIX = "verification:code:";

    private static final String VERIFICATION_ATTEMPT_PREFIX = "verification:attempt:";

    private final EmailVerificationProperties emailVerificationProperties;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码
     */
    public String generateCode() {
        return RandomUtil.randomNumbers(emailVerificationProperties.getCodeLength());
    }

    /**
     * 保存验证码到Redis，5分钟过期
     */
    public void saveVerificationCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(key, code, Duration.ofMinutes(emailVerificationProperties.getCodeExpireMinutes()));
        // 记录发送次数（可选，用于限流）
        String attemptKey = VERIFICATION_ATTEMPT_PREFIX + email;
        stringRedisTemplate.opsForValue().increment(attemptKey);
        stringRedisTemplate.expire(attemptKey, Duration.ofHours(1));
    }

    /**
     * 验证验证码
     */
    public boolean verifyCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = stringRedisTemplate.opsForValue().get(key);
        // 先判断 storedCode 是否为 null，避免 NPE
        if (storedCode == null) {
            return false; // 验证码不存在或已过期
        }
        if (code.equals(storedCode)) {
            // 验证成功后删除验证码
            stringRedisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * 获取验证码（用于测试和调试）
     */
    public String getCode(String email) {
        String key = VERIFICATION_CODE_PREFIX + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 检查发送次数（限流）
     */
    public boolean isAllowedToSend(String email) {
        String attemptKey = VERIFICATION_ATTEMPT_PREFIX + email;
        String attempts = stringRedisTemplate.opsForValue().get(attemptKey);
        int maxAttemptsPerHour = emailVerificationProperties.getMaxAttemptsPerHour();
        return attempts == null || Integer.parseInt(attempts) < maxAttemptsPerHour;
    }
}
