package com.lwh.pictureproject.manager;

import cn.hutool.core.io.FileUtil;
import com.lwh.pictureproject.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lin
 * @version 1.0.0
 * @description 最基础的文件上传下载
 * @date 2024/12/21 23:23
 */
@Component
@RequiredArgsConstructor
public class CosManager {

    private final CosClientConfig cosClientConfig;

    private final COSClient cosClient;

    /**
     * @param key  唯一键(文件要存到对象存储的哪个位置)
     * @param file 文件
     * @description: 上传对象
     * @author: Lin
     * @date: 2024/12/21 23:25
     * @return: com.qcloud.cos.model.PutObjectResult
     **/
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * @param key 唯一键(哪个路径下的文件)
     * @description: 下载对象
     * @author: Lin
     * @date: 2024/12/21 23:41
     * @return: com.qcloud.cos.model.COSObject
     **/
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * @param key  唯一键(文件要存到对象存储的哪个位置)
     * @param file 文件
     * @description: 上传对象（附带图片信息）
     * @author: Lin
     * @date: 2024/12/22 0:21
     * @return: com.qcloud.cos.model.PutObjectResult
     **/
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 2025.1.6 新增对图片进行处理，转换为webp格式
        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 1.添加图片压缩规则（生成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 2. 添加缩略图处理规则（仅对 > 20 KB 的图片生成缩略图）
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        String host = cosClientConfig.getHost();
        key = key.replace(host, "").replaceFirst("^/", "");
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}