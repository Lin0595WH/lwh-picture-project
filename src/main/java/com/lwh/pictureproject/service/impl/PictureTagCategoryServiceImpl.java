package com.lwh.pictureproject.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lwh.pictureproject.mapper.PictureTagCategoryMapper;
import com.lwh.pictureproject.model.entity.PictureTagCategory;
import com.lwh.pictureproject.model.vo.PictureTagCategoryVO;
import com.lwh.pictureproject.service.PictureTagCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lin
 * @description 针对表【picture_tag_category(图片标签分类表)】的数据库操作Service实现
 * @createDate 2025-10-13 21:05:31
 */
@Service
public class PictureTagCategoryServiceImpl extends ServiceImpl<PictureTagCategoryMapper, PictureTagCategory>
        implements PictureTagCategoryService {

    /**
     * @Descriptions 获取图片标签和列表
     * @Date 2025/10/13 21:12
     * @Author Lin
     */
    @Override
    public PictureTagCategoryVO getPictureTagCategoryVO() {
        // 获取全部数据并处理空集合（避免后续流处理空指针）
        List<PictureTagCategory> tagCategories = this.list();
        List<PictureTagCategory> nonNullList = CollUtil.isEmpty(tagCategories) ? List.of() : tagCategories;
        // 用Stream流拆分标签和分类，避免显式循环
        List<String> tagList = nonNullList.stream()
                .filter(item -> item.getType() == 1)
                .map(PictureTagCategory::getName)
                .collect(Collectors.toList());
        List<String> categoryList = nonNullList.stream()
                .filter(item -> item.getType() == 2)
                .map(PictureTagCategory::getName)
                .collect(Collectors.toList());
        // 直接构建返回（因VO已设置默认空列表，这里即使集合为空也无需特殊处理）
        return PictureTagCategoryVO.builder()
                .tagList(tagList)
                .categoryList(categoryList)
                .build();
    }
}




