package com.lwh.pictureproject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.mapper.SpaceUserMapper;
import com.lwh.pictureproject.model.dto.spaceuser.SpaceUserAddRequest;
import com.lwh.pictureproject.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.SpaceUser;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.SpaceRoleEnum;
import com.lwh.pictureproject.model.vo.SpaceUserVO;
import com.lwh.pictureproject.model.vo.SpaceVO;
import com.lwh.pictureproject.model.vo.UserVO;
import com.lwh.pictureproject.service.SpaceService;
import com.lwh.pictureproject.service.SpaceUserService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lin
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-09-15 21:51:38
 */
@Service
@RequiredArgsConstructor
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    private final UserService userService;

    @Lazy
    private final SpaceService spaceService;

    /**
     * @param spaceUserAddRequest 空间成员创建请求
     * @return long
     * @Descriptions 创建空间成员
     * @Date 2025/9/15 22:44
     * @Author Lin
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        this.validSpaceUser(spaceUser, true);
        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * @param spaceUser 空间用户
     * @param add       是否为创建时检验
     * @Descriptions 校验空间成员
     * @Date 2025/9/15 22:43
     * @Author Lin
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 判断用户是否已经在该空间了
            SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
            BeanUtil.copyProperties(spaceUser, spaceUserQueryRequest);
            LambdaQueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
            boolean exists = this.exists(queryWrapper);
            if (exists) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已经加入该空间");
            }
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = EnumUtil.getBy(SpaceRoleEnum::getValue, spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    /**
     * @param spaceUser 空间用户
     * @return com.lwh.pictureproject.model.vo.SpaceUserVO
     * @Descriptions 获取空间成员包装类（单条）
     * @Date 2025/9/15 22:39
     * @Author Lin
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * @param spaceUserList 空间用户列表
     * @return java.util.List<com.lwh.pictureproject.model.vo.SpaceUserVO>
     * @Descriptions 获取空间成员包装类（列表）
     * @Date 2025/9/15 22:32
     * @Author Lin
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId)
                .filter(ObjectUtil::isNotEmpty).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId)
                .filter(ObjectUtil::isNotEmpty).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    /**
     * @param spaceUserQueryRequest 空间用户查询请求
     * @return com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.lwh.pictureproject.model.entity.SpaceUser>
     * @Descriptions 获取查询对象
     * @Date 2025/9/15 22:25
     * @Author Lin
     */
    @Override
    public LambdaQueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 2.获取查询参数
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = EnumUtil.getBy(SpaceRoleEnum::getValue, spaceRole);
        // 3.创建查询条件
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), SpaceUser::getId, id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId), SpaceUser::getSpaceId, spaceId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), SpaceUser::getUserId, userId);
        queryWrapper.eq(ObjectUtil.isAllNotEmpty(spaceRole, spaceRoleEnum), SpaceUser::getSpaceRole, spaceRoleEnum.getValue());
        return queryWrapper;
    }
}




