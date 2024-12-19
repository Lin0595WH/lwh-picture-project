package com.lwh.pictureproject.constant;

/**
 * @author Lin
 * @version 1.0.0
 * @description 用户常量
 * @date 2024/12/18 22:44
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    // endregion

    String DEFAULT_PASSWORD = "123456";
}