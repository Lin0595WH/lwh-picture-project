package com.lwh.pictureproject.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Lin
 * @version 1.0.0
 * @description 用户创建请求
 * @date 2024/12/19 20:51
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 4890295723303674177L;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;
}