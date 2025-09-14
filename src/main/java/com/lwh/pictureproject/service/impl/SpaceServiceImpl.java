package com.lwh.pictureproject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.mapper.SpaceMapper;
import com.lwh.pictureproject.model.dto.space.SpaceAddRequest;
import com.lwh.pictureproject.model.dto.space.SpaceQueryRequest;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.SpaceLevelEnum;
import com.lwh.pictureproject.model.enums.SpaceTypeEnum;
import com.lwh.pictureproject.model.vo.SpaceVO;
import com.lwh.pictureproject.model.vo.UserVO;
import com.lwh.pictureproject.service.SpaceService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Lin
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-01-13 20:54:30
 */
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    private final UserService userService;

    private final TransactionTemplate transactionTemplate;

    private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * @param spaceAddRequest 创建空间请求
     * @param loginUser       当前登录用户
     * @description: 创建空间
     * @author: Lin
     * @date: 2025/1/16 21:11
     * @return: long
     **/
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 填充默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 校验参数
        this.validSpace(space, true);
        // 校验权限,普通用户只能创建普通的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        boolean admin = userService.isAdmin(loginUser);
        ThrowUtils.throwIf(!admin && SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel(),
                ErrorCode.PARAMS_ERROR, "无权限创建高级空间");
        // 4. 控制同一用户只能创建一个私有空间
        // 针对用户进行加锁
        // 这种使用字符串常量池的加锁方式，数据不会及时释放，优化为使用ConcurrentHashMap的方式
        //String lock = String.valueOf(userId).intern();
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        try {
            synchronized (lock) {
                return Optional.ofNullable(transactionTemplate.execute(status -> {
                    try {
                        // 判断是否已有空间
                        boolean exists = this.lambdaQuery()
                                .eq(Space::getUserId, userId)
                                .exists();
                        // 如果已有空间，就不能再创建
                        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                        // 创建
                        boolean result = this.save(space);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                        // 返回新写入的数据 id
                        return space.getId();
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        log.error("创建空间失败", e);
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
                    }
                })).orElse(-1L);
            }
        } finally {
            lockMap.remove(userId);
        }
    }

    /**
     * @param space 空间
     * @param add 是否为创建时检验
     * @description: 校验空间
     * @author: Lin
     * @date: 2025/1/13 21:11
     * @return: void
     **/
    @Override
    public void validSpace(Space space, boolean add) {
        // 校验
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "space 为空");
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
        }
        // 修改数据时，空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改数据时，空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 修改数据时，空间类别进行校验
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }

    /**
     * @param space   原对象
     * @param request 请求
     * @description: space 转换为 VO
     * @author: Lin
     * @date: 2025/1/13 21:27
     * @return: com.lwh.pictureproject.model.vo.SpaceVO
     **/
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * @param spacePage 分页数据
     * @param request   请求
     * @description: 获取分页数据封装类
     * @author: Lin
     * @date: 2025/1/13 21:30
     * @return: com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.lwh.pictureproject.model.vo.SpaceVO>
     **/
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, User> userIdUserMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = userIdUserMap.get(userId);
            spaceVO.setUser(user != null ? userService.getUserVO(user) : null);
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * @param spaceQueryRequest 查询条件
     * @description: space构造查询条件
     * @author: Lin
     * @date: 2025/1/13 21:36
     * @return: com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.lwh.pictureproject.model.entity.Space>
     **/
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "space_level", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * @param space 空间对象
     * @description: 根据空间级别填充空间对象
     * @author: Lin
     * @date: 2025/1/13 21:41
     * @return: void
     **/
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * @param loginUser 当前登录用户
     * @param space     空间对象
     * @description: 校验当前用户是否有空间权限
     * @author: Lin
     * @date: 2025/1/16 22:42
     * @return: void
     **/
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前空间不存在");
        }
        // 空间的用户id和当前登录用户id是否一致
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        }
    }
}




