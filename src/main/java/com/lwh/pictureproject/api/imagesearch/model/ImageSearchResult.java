package com.lwh.pictureproject.api.imagesearch.model;

import lombok.Data;

/**
 * @author Lin
 * @version 1.0.0
 * @description 图片搜索结果
 * @date 2025/3/10 20:26
 */
@Data
public class ImageSearchResult {
    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
