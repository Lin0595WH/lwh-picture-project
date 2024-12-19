package com.lwh.pictureproject.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Lin
 * @version 1.0.0
 * @description 用户登录请求
 * @date 2024/12/18 22:18
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -351247892128586980L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
