package com.lwh.pictureproject.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lin
 * @version 1.0.0
 * @description 图片标签分类列表视图
 * @date 2024/12/24 22:21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureTagCategoryVO {

    /**
     * 标签列表
     */
    @Builder.Default
    private List<String> tagList = new ArrayList<>();


    /**
     * 分类列表
     */
    @Builder.Default
    private List<String> categoryList = new ArrayList<>();

}