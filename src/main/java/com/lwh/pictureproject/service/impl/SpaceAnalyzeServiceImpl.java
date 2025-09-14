package com.lwh.pictureproject.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.mapper.SpaceMapper;
import com.lwh.pictureproject.model.dto.space.analyze.*;
import com.lwh.pictureproject.model.entity.Picture;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.vo.space.analyze.*;
import com.lwh.pictureproject.service.PictureService;
import com.lwh.pictureproject.service.SpaceAnalyzeService;
import com.lwh.pictureproject.service.SpaceService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @Author Lin
 * @Date 2025/9/12 21:23
 * @Descriptions 空间使用情况分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    private final UserService userService;

    private final SpaceService spaceService;

    private final PictureService pictureService;

    /**
     * @param spaceUsageAnalyzeRequest 空间使用情况分析请求封装类
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse
     * @Descriptions 获取空间使用情况分析
     * @Date 2025/9/12 22:07
     * @Author Lin
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 1.校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        // 公共或全部
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(Picture::getPicSize);
            this.fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            List<Object> pictureList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = 0L;
            long usddCount = 0L;
            if (CollUtil.isNotEmpty(pictureList)) {
                usedSize = pictureList.stream().mapToLong(Long.class::cast).sum();
                usddCount = pictureList.size();
            }
            return SpaceUsageAnalyzeResponse.builder()
                    .usedSize(usedSize)
                    .maxSize(null)
                    .sizeUsageRatio(null)
                    .usedCount(usddCount)
                    .maxCount(null)
                    .countUsageRatio(null)
                    .build();
        }
        // 指定空间
        else {
            // 2.构造查询条件
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            Long totalSize = space.getTotalSize();
            Long maxSize = space.getMaxSize();
            // 计算空间使用率
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            Long totalCount = space.getTotalCount();
            Long maxCount = space.getMaxCount();
            // 计算图片数量占比
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            return SpaceUsageAnalyzeResponse.builder()
                    .usedSize(totalSize)
                    .maxSize(maxSize)
                    .sizeUsageRatio(sizeUsageRatio)
                    .usedCount(totalCount)
                    .maxCount(maxCount)
                    .countUsageRatio(countUsageRatio)
                    .build();
        }
    }

    /**
     * @param spaceCategoryAnalyzeRequest 空间分类使用情况分析请求封装类
     * @param loginUser                   当前登录用户
     * @Descriptions 获取空间分类使用情况分析
     * @return: SpaceCategoryAnalyzeResponse
     * @Date 2025/9/12 22:42
     * @Author Lin
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 1.校验
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        // 使用 MyBatis Plus 分组查询
        queryWrapper.select("category", "count(*) as count", "sum(pic_size) as totalSize")
                .groupBy("category")
                .orderByDesc("count");
        // 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * @param spaceTagAnalyzeRequest 获取空间标签使用情况分析请求封装类
     * @param loginUser              当前登录用户
     * @return java.util.List<com.lwh.pictureproject.model.vo.space.analyze.SpaceTagAnalyzeResponse>
     * @Descriptions 获取空间标签使用情况分析
     * @Date 2025/9/13 16:21
     * @Author Lin
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 校验
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构造查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select(Picture::getTags)
                .isNotNull(Picture::getTags);
        // 获取并处理结果
        List<Object> pictureList = pictureService.getBaseMapper().selectObjs(queryWrapper);
        if (CollUtil.isNotEmpty(pictureList)) {
            Map<String, Long> tagCountMap = pictureList.stream()
                    .filter(ObjUtil::isNotNull)
                    .map(Object::toString)
                    .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                    .filter(tag -> tag != null && !tag.isEmpty())
                    .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
            return tagCountMap.entrySet().stream()
                    .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }

    /**
     * @param spaceSizeAnalyzeRequest 获取空间大小使用情况分析请求封装类
     * @param loginUser               当前登录用户
     * @return java.util.List<com.lwh.pictureproject.model.vo.space.analyze.SpaceSizeAnalyzeResponse>
     * @Descriptions 获取空间大小使用情况分析
     * @Date 2025/9/13 16:49
     * @Author Lin
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 校验
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select(Picture::getPicSize)
                .isNotNull(Picture::getPicSize)
                .orderByDesc(Picture::getPicSize);
        List<Object> objects = pictureService.getBaseMapper().selectObjs(queryWrapper);
        if (CollUtil.isNotEmpty(objects)) {
            // 定义分段范围
            Map<String, Long> sizeRanges = new LinkedHashMap<>();
            sizeRanges.put("<100KB", 0L);
            sizeRanges.put("100KB-500KB", 0L);
            sizeRanges.put("500KB-1MB", 0L);
            sizeRanges.put(">1MB", 0L);
            objects.stream()
                    .filter(ObjUtil::isNotNull)
                    .map(size -> ((Number) size).longValue())
                    .forEach(size -> {
                        // 按区间更新计数，因已预设key，get()不会返回null
                        if (size < 100 * 1024) {
                            sizeRanges.put("<100KB", sizeRanges.get("<100KB") + 1);
                        } else if (size < 500 * 1024) {
                            sizeRanges.put("100KB-500KB", sizeRanges.get("100KB-500KB") + 1);
                        } else if (size < 1024 * 1024) {
                            sizeRanges.put("500KB-1MB", sizeRanges.get("500KB-1MB") + 1);
                        } else {
                            sizeRanges.put(">1MB", sizeRanges.get(">1MB") + 1);
                        }
                    });
            return sizeRanges.entrySet().stream()
                    .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }

    /**
     * @param spaceUserAnalyzeRequest 获取空间用户上传行为分析请求封装类
     * @param loginUser               当前登录用户
     * @return java.util.List<com.lwh.pictureproject.model.vo.space.analyze.SpaceUserAnalyzeResponse>
     * @Descriptions 获取空间用户上传行为分析
     * @Date 2025/9/13 17:15
     * @Author Lin
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 校验
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        // 补充用户 id 查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        // 补充分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(create_time) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        // 分组排序
        queryWrapper.groupBy("period").orderByAsc("period");
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return Optional.ofNullable(queryResult)
                .orElse(Collections.emptyList())
                .stream()
                .map(result -> new SpaceUserAnalyzeResponse(
                        result.get("period").toString(),
                        ((Number) result.get("count")).longValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * @param spaceRankAnalyzeRequest 获取空间使用排行分析请求封装类
     * @param loginUser               当前登录用户
     * @return java.util.List<com.lwh.pictureproject.model.entity.Space>
     * @Descriptions 获取空间使用排行分析（仅管理员）
     * @Date 2025/9/13 17:38
     * @Author Lin
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null
                        || spaceRankAnalyzeRequest.getTopN() == null
                        || spaceRankAnalyzeRequest.getTopN() <= 0
                        || spaceRankAnalyzeRequest.getTopN() >= 50,
                ErrorCode.PARAMS_ERROR);
        // 检查权限，仅管理员可以查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        // 构造查询条件
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Space::getId, Space::getSpaceName, Space::getUserId, Space::getTotalSize)
                .orderByDesc(Space::getTotalSize)
                .last("limit " + spaceRankAnalyzeRequest.getTopN());
        // 查询并封装结果
        return spaceService.list(queryWrapper);
    }

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeRequest 空间分析请求
     * @param loginUser           登录用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null || loginUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        boolean admin = userService.isAdmin(loginUser);
        // 不能同时查询公共空间和全部空间
        ThrowUtils.throwIf(queryPublic && queryAll, ErrorCode.PARAMS_ERROR);
        // 查公共/全部：要管理员
        ThrowUtils.throwIf((queryPublic || queryAll) && !admin, ErrorCode.NO_AUTH_ERROR);
        // 先看空间存不存在
        ThrowUtils.throwIf(!queryPublic && !queryAll && (spaceId == null || spaceId <= 0), ErrorCode.PARAMS_ERROR);
        if (!queryPublic && !queryAll) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 查空间：空间创建人或者是管理员
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据请求对象封装查询条件
     *
     * @param spaceAnalyzeRequest 空间分析请求
     * @param queryWrapper        查询条件
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, LambdaQueryWrapper<Picture> queryWrapper) {
        // 查看全部空间，包括私有空间和公共空间
        if (spaceAnalyzeRequest.isQueryAll()) {
            // 无需条件
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            // 公共空间：spaceId为null
            queryWrapper.isNull(Picture::getSpaceId);
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            // 指定私有空间：spaceId等于指定值（已提前校验非null）
            queryWrapper.eq(Picture::getSpaceId, spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 根据请求对象封装查询条件
     *
     * @param spaceAnalyzeRequest 空间分析请求
     * @param queryWrapper        查询条件
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 查看全部空间，包括私有空间和公共空间
        if (spaceAnalyzeRequest.isQueryAll()) {
            // 无需条件
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            // 公共空间：spaceId为null
            queryWrapper.isNull("space_id");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            // 指定私有空间：spaceId等于指定值（已提前校验非null）
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }
}
