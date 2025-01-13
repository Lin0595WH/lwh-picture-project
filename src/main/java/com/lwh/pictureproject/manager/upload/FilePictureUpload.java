package com.lwh.pictureproject.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lwh.pictureproject.config.CosClientConfig;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.manager.CosManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Set;

/**
 * @author Lin
 * @version 1.0.0
 * @description 文件图片上传
 * @date 2025/1/1 14:23
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    public FilePictureUpload(CosClientConfig cosClientConfig, CosManager cosManager) {
        super(cosClientConfig, cosManager);
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表（或者集合）
        final Set<String> ALLOW_IMAGE_SUFFIXES = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff",
                "svg", "webp", "ico", "raw");
        ThrowUtils.throwIf(!ALLOW_IMAGE_SUFFIXES.contains(fileSuffix), ErrorCode.PARAMS_ERROR,
                "不支持的图片格式：" + fileSuffix);
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
