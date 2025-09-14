package com.lwh.pictureproject.model.dto.space.analyze;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author Lin
 * @Date 2025/9/13 17:13
 * @Descriptions 空间用户上传行为分析请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}
