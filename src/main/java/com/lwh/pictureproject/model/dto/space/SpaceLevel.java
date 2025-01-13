package com.lwh.pictureproject.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Lin
 * @version 1.0.0
 * @description 空间级别
 * @date 2025/1/13 20:59
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 值
     */
    private int value;

    /**
     * 中文
     */
    private String text;

    /**
     * 最大数量
     */
    private long maxCount;

    /**
     * 最大容量
     */
    private long maxSize;
}