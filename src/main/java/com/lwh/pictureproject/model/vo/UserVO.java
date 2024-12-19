package com.lwh.pictureproject.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Lin
 * @version 1.0.0
 * @description 返回用户信息（脱敏后）
 * @date 2024/12/19 20:56
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = -4756431753790148415L;

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;
}
