package com.lwh.pictureproject.controller;


import com.lwh.pictureproject.common.BaseResponse;
import com.lwh.pictureproject.common.ResultUtils;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.model.dto.space.analyze.*;
import com.lwh.pictureproject.model.entity.Space;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.vo.space.analyze.*;
import com.lwh.pictureproject.service.SpaceAnalyzeService;
import com.lwh.pictureproject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author Lin
 * @Date 2025/9/12 22:36
 * @Descriptions 空间分析请求
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    private final SpaceAnalyzeService spaceAnalyzeService;

    private final UserService userService;

    /**
     * @param spaceUsageAnalyzeRequest 获取空间使用情况请求
     * @param request                  Http请求
     * @Author Lin
     * @Date 2025/9/12 22:37
     * @Descriptions 获取空间使用情况
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceUsageAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser));
    }


    /**
     * @param spaceCategoryAnalyzeRequest 获取空间分类分析请求
     * @param request                     Http请求
     * @Descriptions 获取空间图片分类分析
     * @Date 2025/9/12 23:24
     * @Author Lin
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceCategoryAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser));
    }

    /**
     * @param spaceTagAnalyzeRequest 获取空间图片标签分析请求
     * @param request                Http请求
     * @Descriptions 获取空间图片标签分析
     * @Date 2025/9/13 16:39
     * @Author Lin
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceTagAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser));
    }

    /**
     * @param spaceSizeAnalyzeRequest 获取空间图片大小分析请求
     * @param request                 Http请求
     * @Descriptions 获取空间图片大小分析
     * @Date 2025/9/13 17:11
     * @Author Lin
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
            @RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceSizeAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser));
    }

    /**
     * @param spaceUserAnalyzeRequest 获取空间用户行为分析请求
     * @param request                 Http请求
     * @Descriptions 获取空间用户行为分析
     * @Date 2025/9/13 17:39
     * @Author Lin
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
            @RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceUserAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser));
    }

    /**
     * 获取空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 获取空间使用排行分析请求
     * @param request                 Http请求
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                         HttpServletRequest request) {
        User loginUser = this.checkRequestAndGetLoginUser(spaceRankAnalyzeRequest, request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser));
    }


    /**
     * 封装通用的检验逻辑
     *
     * @param requestDTO 接口请求参数DTO
     * @param request    Http请求对象
     * @return 登录用户（非null）
     */
    private User checkRequestAndGetLoginUser(Object requestDTO, HttpServletRequest request) {
        // 1. 校验请求体和Http请求对象非空
        ThrowUtils.throwIf(requestDTO == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "Http请求对象不能为空");
        // 2. 校验登录用户存在
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录，请先登录");
        // 3. 校验通过，返回登录用户
        return loginUser;
    }

}
