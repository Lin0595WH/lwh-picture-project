package com.lwh.pictureproject.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Lin
 * @version 1.0.0
 * @description 更新用户请求
 * @date 2024/12/19 20:51
 */
@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 4563269354250078522L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}