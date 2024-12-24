package com.lwh.pictureproject.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.lwh.pictureproject.config.CosClientConfig;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.Set;

/**
 * @author Lin
 * @version 1.0.0
 * @description 通用的文件上传下载
 * @date 2024/12/22 0:16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileManager {

    private final CosClientConfig cosClientConfig;

    private final CosManager cosManager;

    final Set<String> ALLOW_IMAGE_SUFFIXES = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff",
            "svg", "webp", "ico", "raw");

    /**
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @description: 上传图片
     * @author: Lin
     * @date: 2024/12/24 20:20
     * @return: com.lwh.pictureproject.model.dto.file.UploadPictureResult
     **/
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1.校验图片
        String fileSuffix = this.validPicture(multipartFile);
        // 2.图片上传路径
        String uuid = RandomUtil.randomString(8);
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);
        // 真正的文件上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 3.解析结果并返回
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 将multipartFile写入临时文件
            multipartFile.transferTo(file);
            // 上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 返回的图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 返回封装结果
            // 返回可访问的地址
            return UploadPictureResult.builder().url(cosClientConfig.getHost() + "/" + uploadPath).picName(FileUtil.mainName(multipartFile.getOriginalFilename())).picSize(FileUtil.size(file)).picHeight(imageInfo.getHeight()).picWidth(imageInfo.getWidth()).picFormat(imageInfo.getFormat()).picScale(NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue()).build();
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * @param multipartFile 文件
     * @description: 校验图片
     * @author: Lin
     * @date: 2024/12/24 20:21
     * @return: void
     **/
    private String validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传的文件multipartFile 为空");
        // 1.校验文件大小 （先限制为5MB）
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "上传文件大小超过5MB");
        // 2.校验文件后缀
        String fileName = multipartFile.getOriginalFilename();
        ThrowUtils.throwIf(fileName == null, ErrorCode.PARAMS_ERROR, "上传文件名称为空");
        String suffix = FileUtil.getSuffix(fileName);
        ThrowUtils.throwIf(!ALLOW_IMAGE_SUFFIXES.contains(suffix), ErrorCode.PARAMS_ERROR, "不支持的图片格式：" + suffix);
        // 3.返回文件后缀
        return suffix;
    }

    /**
     * @param file 临时文件
     * @description: 临时文件清理
     * @author: Lin
     * @date: 2024/12/24 21:13
     * @return: void
     **/
    private static void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
