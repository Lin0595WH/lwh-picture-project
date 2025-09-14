package com.lwh.pictureproject.model.vo.space.analyze;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Lin
 * @Date 2025/9/12 22:00
 * @Descriptions 空间资源使用分析响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceUsageAnalyzeResponse implements Serializable {

    /**
     * 已使用大小
     */
    private Long usedSize;

    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 当前图片数量
     */
    private Long usedCount;

    /**
     * 最大图片数量
     */
    private Long maxCount;

    /**
     * 图片数量占比
     */
    private Double countUsageRatio;

    private static final long serialVersionUID = 1L;
}
