package com.lwh.pictureproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lwh.pictureproject.model.entity.PictureTagCategory;
import com.lwh.pictureproject.model.vo.PictureTagCategoryVO;

/**
 * @author Lin
 * @description 针对表【picture_tag_category(图片标签分类表)】的数据库操作Service
 * @createDate 2025-10-13 21:05:31
 */
public interface PictureTagCategoryService extends IService<PictureTagCategory> {

    /**
     * 获取图片标签和列表
     **/
    PictureTagCategoryVO getPictureTagCategoryVO();
}
