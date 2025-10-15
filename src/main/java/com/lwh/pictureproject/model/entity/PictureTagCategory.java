package com.lwh.pictureproject.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图片标签分类表
 *
 * @TableName picture_tag_category
 */
@TableName(value = "picture_tag_category")
@Data
public class PictureTagCategory {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型(1标签2分类)
     */
    private Integer type;

    /**
     * 名称
     */
    private String name;

    /**
     * 排序号
     */
    private Integer sortNum;
}
