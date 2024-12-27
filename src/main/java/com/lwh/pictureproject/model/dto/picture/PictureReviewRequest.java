package com.lwh.pictureproject.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Lin
 * @version 1.0.0
 * @description 图片审核请求
 * @date 2024/12/27 22:23
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
