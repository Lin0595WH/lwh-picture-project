package com.lwh.pictureproject.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.dto.spaceuser.SpaceUserAddRequest;
import com.lwh.pictureproject.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lwh.pictureproject.model.entity.SpaceUser;
import com.lwh.pictureproject.model.vo.SpaceUserVO;

import java.util.List;

/**
 * @author Lin
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-09-15 21:51:38
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 空间成员创建请求
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser 空间用户
     * @param add       是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser 空间用户
     * @return 空间成员响应类
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList 空间用户列表
     * @return 空间成员响应类列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest 空间成员查询请求
     * @return 查询对象
     */
    LambdaQueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
