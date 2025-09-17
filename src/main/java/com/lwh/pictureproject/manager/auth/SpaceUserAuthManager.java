package com.lwh.pictureproject.manager.auth;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.json.JSONUtil;
import com.lwh.pictureproject.manager.auth.model.SpaceUserAuthConfig;
import com.lwh.pictureproject.manager.auth.model.SpaceUserPermissionConstant;
import com.lwh.pictureproject.manager.auth.model.SpaceUserRole;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.SpaceUser;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.SpaceRoleEnum;
import com.lwh.pictureproject.model.enums.SpaceTypeEnum;
import com.lwh.pictureproject.service.SpaceUserService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @Author Lin
 * @Date 2025/9/16 21:17
 * @Descriptions 空间成员权限管理
 */
@Component
@RequiredArgsConstructor
public class SpaceUserAuthManager {

    private final UserService userService;

    private final SpaceUserService spaceUserService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * @param spaceUserRole 空间用户角色
     * @return java.util.List<java.lang.String>
     * @Descriptions 根据角色获取权限列表
     * @Date 2025/9/16 21:20
     * @Author Lin
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (CharSequenceUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }
        return SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(role -> role.getKey().equals(spaceUserRole))
                .findFirst()
                .map(SpaceUserRole::getPermissions)
                .orElse(Collections.emptyList());
    }

    /**
     * @param space     空间
     * @param loginUser 当前登录用户
     * @return java.util.List<java.lang.String>
     * @Descriptions 获取权限列表
     * @Date 2025/9/16 21:24
     * @Author Lin
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return Collections.emptyList();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = this.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        // 剩下就是私人空间或团队空间了
        SpaceTypeEnum spaceTypeEnum = EnumUtil.getBy(SpaceTypeEnum::getValue, space.getSpaceType());
        if (spaceTypeEnum == null) {
            // 不正确的空间类型
            return Collections.emptyList();
        }
        // 根据空间获取对应的权限
        // 私人空间处理逻辑
        if (SpaceTypeEnum.PRIVATE.equals(spaceTypeEnum)) {
            // 空间创建者或者系统管理员 拥有所有权限
            if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                // 其他人都没有权限
                return Collections.emptyList();
            }
        }
        // 团队空间处理逻辑
        else if (SpaceTypeEnum.TEAM.equals(spaceTypeEnum)) {
            SpaceUser spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, space.getId())
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .one();
            // 未找到此空间
            if (spaceUser == null) {
                return Collections.emptyList();
            } else {
                // 根据这个人的角色返回权限
                return this.getPermissionsByRole(spaceUser.getSpaceRole());
            }
        }
        return Collections.emptyList();
    }
}
