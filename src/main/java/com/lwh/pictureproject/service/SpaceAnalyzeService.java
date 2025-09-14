package com.lwh.pictureproject.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.dto.space.analyze.*;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.vo.space.analyze.*;

import java.util.List;

/**
 * @Author Lin
 * @Date 2025/9/12 21:22
 * @Descriptions 空间使用情况分析
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用情况分析
     *
     * @param spaceUsageAnalyzeRequest 空间使用情况分析请求封装类
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse
     **/
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类使用情况分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类使用情况分析请求封装类
     * @param loginUser                   当前登录用户
     * @return: List<SpaceCategoryAnalyzeResponse>
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间标签使用情况分析
     *
     * @param spaceTagAnalyzeRequest 获取空间标签使用情况分析请求封装类
     * @param loginUser              当前登录用户
     * @return: List<SpaceTagAnalyzeResponse>
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小使用情况分析
     *
     * @param spaceSizeAnalyzeRequest 获取空间大小使用情况分析请求封装类
     * @param loginUser               当前登录用户
     * @return: List<SpaceSizeAnalyzeResponse>
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 获取空间用户上传行为分析请求封装类
     * @param loginUser               当前登录用户
     * @return: List<SpaceUserAnalyzeResponse>
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用排行分析（仅管理员）
     *
     * @param spaceRankAnalyzeRequest 获取空间使用排行分析请求封装类
     * @param loginUser               当前登录用户
     * @return java.util.List<com.lwh.pictureproject.model.entity.Space>
     **/
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);

}


