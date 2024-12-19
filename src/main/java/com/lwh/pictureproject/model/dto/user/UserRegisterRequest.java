package com.lwh.pictureproject.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Lin
 * @version 1.0.0
 * @description 用户注册请求
 * @date 2024/12/18 21:21
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3196741210361544227L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
