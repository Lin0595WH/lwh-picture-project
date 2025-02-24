package com.lwh.pictureproject.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.dto.space.SpaceAddRequest;
import com.lwh.pictureproject.model.dto.space.SpaceQueryRequest;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Lin
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-01-13 20:54:30
 */
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间包装类（单条）
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     */
    void checkSpaceAuth(User loginUser, Space space);
}
