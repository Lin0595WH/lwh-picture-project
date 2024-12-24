package com.lwh.pictureproject.manager;

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
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

}