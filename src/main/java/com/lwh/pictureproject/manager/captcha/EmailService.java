package com.lwh.pictureproject.manager.captcha;


import cn.hutool.core.lang.Validator;
import cn.hutool.core.text.CharSequenceUtil;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.manager.captcha.model.EmailVerificationProperties;
import com.lwh.pictureproject.manager.captcha.model.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * @Author Lin
 * @Date 2025/10/10 20:58
 * @Descriptions 邮件服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;

    private final EmailVerificationProperties verificationProperties;

    /**
     * 发送验证码邮件
     */
    public void sendCaptcha(String toEmail, String code) {
        // 1. 参数校验（避免空指针或无效邮箱）
        ThrowUtils.throwIf(CharSequenceUtil.hasBlank(toEmail, code), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!Validator.isEmail(toEmail), ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        ThrowUtils.throwIf(code.length() != 6, ErrorCode.PARAMS_ERROR, "验证码长度错误");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(toEmail);
            message.setSubject("老林云图库注册验证码");
            message.setText(this.buildEmailContent(code));
            mailSender.send(message);
        } catch (MailSendException e) {
            // 邮件发送失败（如服务器异常、收件人不存在等）
            log.error("验证码邮件发送失败，收件人：{}，错误信息：{}", toEmail, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮件发送失败，请检查邮箱是否有效");
        } catch (Exception e) {
            // 其他未知异常
            log.error("发送验证码邮件时发生未知错误，收件人：{}", toEmail, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统异常，邮件发送失败");
        }
    }

    private String buildEmailContent(String code) {
        int expireMinutes = verificationProperties.getCodeExpireMinutes();
        return CharSequenceUtil.format(
                "【老林云图库】您的注册验证码\n\n" +
                        "验证码：{}\n" +
                        "有效期：{}分钟\n\n" +
                        "请勿将此验证码泄露给他人。如非本人操作，请忽略此邮件。",
                code,
                expireMinutes
        );
    }
}
