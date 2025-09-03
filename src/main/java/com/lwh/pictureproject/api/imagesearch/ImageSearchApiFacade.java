package com.lwh.pictureproject.api.imagesearch;

import com.lwh.pictureproject.api.imagesearch.model.ImageSearchResult;
import com.lwh.pictureproject.api.imagesearch.sub.GetImageFirstUrlApi;
import com.lwh.pictureproject.api.imagesearch.sub.GetImageListApi;
import com.lwh.pictureproject.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

/**
 * @author Lin
 * @version 1.0.0
 * @description 使用门面模式，整合 调用 百度的以图搜图 的三个步骤，提供一个统一的接口
 * @date 2025/3/10 20:37
 */
public class ImageSearchApiFacade {
    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageSearchResults = searchImage("https://www.codefather.cn/logo.png");
    }
}
