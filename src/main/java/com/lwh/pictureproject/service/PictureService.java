package com.lwh.pictureproject.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.dto.picture.*;
import com.lwh.pictureproject.model.entity.Picture;
import com.lwh.pictureproject.model.entity.User;
import com.lwh.pictureproject.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Lin
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-21 23:58:47
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     **/
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片包装类（单条）
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询对象
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 图片审核参数填充
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                 User loginUser);

    /**
     * 清理图片文件
     */
    void clearPictureFile(Picture picture);

    /**
     * 校验图片权限
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     */
    void deletePicture(long deletePictureId, User loginUser);

    /**
     * 编辑图片（给用户用）
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
}
