package com.lwh.pictureproject.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @Classname UserRoleEnum
 * @Description 用户角色的枚举类
 * @Version 1.0.0
 * @Date 2024/12/18 20:58
 * @Created by Lin
 */
@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * @description: 根据 value 获取枚举
     * @author: Lin
     * @date: 2024/12/18 21:18
     * @param value 枚举值的 value
     * @return: com.lwh.pictureproject.model.enums.UserRoleEnum 枚举值
     **/
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }
}
