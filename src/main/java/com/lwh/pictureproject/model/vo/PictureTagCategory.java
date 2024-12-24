package com.lwh.pictureproject.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author Lin
 * @version 1.0.0
 * @description 图片标签分类列表视图
 * @date 2024/12/24 22:21
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}