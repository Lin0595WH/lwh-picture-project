package com.lwh.pictureproject.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lwh.pictureproject.annotation.AuthCheck;
import com.lwh.pictureproject.common.BaseResponse;
import com.lwh.pictureproject.common.DeleteRequest;
import com.lwh.pictureproject.common.ResultUtils;
import com.lwh.pictureproject.constant.UserConstant;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.model.dto.picture.*;
import com.lwh.pictureproject.model.entity.Picture;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.enums.PictureReviewStatusEnum;
import com.lwh.pictureproject.model.vo.PictureTagCategory;
import com.lwh.pictureproject.model.vo.PictureVO;
import com.lwh.pictureproject.service.PictureService;
import com.lwh.pictureproject.service.UserService;
import com.lwh.pictureproject.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author Lin
 * @version 1.0.0
 * @description 图片的请求
 * @date 2024/12/24 21:38
 */
@RestController
@RequestMapping("/picture")
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;

    private final UserService userService;

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        // 同时要清除下图片列表的缓存，因为这个时候可能已经不是这些数据了
        CacheUtil.removeCache("lin_picture:listPictureVOByPage:", 1);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过URL上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf((pictureUploadRequest == null || pictureUploadRequest.getFileUrl() == null),
                ErrorCode.PARAMS_ERROR);
        String fileURL = pictureUploadRequest.getFileUrl();
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(fileURL, pictureUploadRequest, loginUser);
        // 同时要清除下图片列表的缓存，因为这个时候可能已经不是这些数据了
        CacheUtil.removeCache("lin_picture:listPictureVOByPage:", 1);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf((deleteRequest == null || deleteRequest.getId() <= 0), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long deletePictureId = deleteRequest.getId();
        // 判断是否存在
        Picture oldPicture = pictureService.getById(deletePictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或者管理员可删除
        ThrowUtils.throwIf((!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)),
                ErrorCode.NO_AUTH_ERROR);
        // 操作数据库
        boolean result = pictureService.removeById(deletePictureId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 清理下对象存储中的图片
        pictureService.clearPictureFile(oldPicture);
        // 同时要清除下图片列表的缓存，因为这个时候可能已经不是这些数据了
        CacheUtil.removeCache("lin_picture:listPictureVOByPage:", 1);
        return ResultUtils.success(true, "图片删除成功");
    }


    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf((pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0),
                ErrorCode.PARAMS_ERROR);
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 2024.12.27 加入审核部分的判断
        pictureService.fillReviewParams(picture, userService.getLoginUser(request));
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 同时要清除下图片列表的缓存，因为这个时候可能已经不是这些数据了
        CacheUtil.removeCache("lin_picture:listPictureVOByPage:", 1);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestBody long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @PostMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestBody long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        if (!admin) {
            // 2024.12.27 加了审核逻辑，所以普通用户只能看审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }
        // 限制爬虫，同时不是管理员才触发此限制
        ThrowUtils.throwIf(size > 20 && !admin, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类）(使用缓存版)
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        // 1.校验
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);
        boolean admin = userService.isAdmin(loginUser);
        if (!admin) {
            // 2024.12.27 加了审核逻辑，所以普通用户只能看审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }
        // 限制爬虫
        ThrowUtils.throwIf(size > 20 && !admin, ErrorCode.PARAMS_ERROR);
        // 2.查库之前先查缓存
        // 2.1构造缓存的key : 将请求参数转为json字符串，然后进行md5加密，作为缓存的key
        String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
        String cacheKey = String.format("lin_picture:listPictureVOByPage:%s", hashKey);
        // 2.从缓存中获取数据
        String cachedValue = CacheUtil.getFromCache(cacheKey);
        if (cachedValue != null) {
            // 命中缓存，直接返回
            return ResultUtils.success(JSONUtil.toBean(cachedValue, Page.class));
        }
        // 3.没命中缓存，则查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 将结果包装为封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 4.将结果存入缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存的过期时间，5 - 10 分钟过期，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        // 写入缓存
        CacheUtil.putToCache(cacheKey, cacheValue, cacheExpireTime);
        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf((pictureEditRequest == null || pictureEditRequest.getId() <= 0), ErrorCode.PARAMS_ERROR);
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        // 判断图片是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        ThrowUtils.throwIf((!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)),
                ErrorCode.PARAMS_ERROR);
        // 2024.12.27 加入审核部分的判断
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 同时要清除下图片列表的缓存，因为这个时候可能已经不是这些数据了
        CacheUtil.removeCache("lin_picture:listPictureVOByPage:", 1);
        return ResultUtils.success(true);
    }

    @PostMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = List.of("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = List.of("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取并创建图片（管理员专用）
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer count = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(count);
    }
}
