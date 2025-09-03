package com.lwh.pictureproject.model.dto.picture;


import lombok.Data;

import java.io.Serializable;

/**
 * @Author Lin
 * @Date 2025/9/3 21:38
 * @Descriptions 按照颜色搜索图片请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
