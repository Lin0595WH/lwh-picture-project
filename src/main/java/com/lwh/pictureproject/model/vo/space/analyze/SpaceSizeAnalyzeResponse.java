package com.lwh.pictureproject.model.vo.space.analyze;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Lin
 * @Date 2025/9/13 16:48
 * @Descriptions 空间图片大小分析响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {

    /**
     * 图片大小范围
     */
    private String sizeRange;

    /**
     * 图片数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
