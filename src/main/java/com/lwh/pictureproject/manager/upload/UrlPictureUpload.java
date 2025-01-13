package com.lwh.pictureproject.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.lwh.pictureproject.config.CosClientConfig;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.exception.ThrowUtils;
import com.lwh.pictureproject.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * @author Lin
 * @version 1.0.0
 * @description URL 图片上传
 * @date 2025/1/1 14:27
 */
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {

    public UrlPictureUpload(CosClientConfig cosClientConfig, CosManager cosManager) {
        super(cosClientConfig, cosManager);
    }

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 1. 校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址为空");
        // 2. 校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            log.error("文件地址格式错误", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        // 3. 校验 URL 的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址"
        );
        // 4. 发送 HEAD 请求验证文件是否存在
        // 有的网站不一定支持head请求， 但这并不表示文件不存在， 所以这里不应该抛异常
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl)
                .execute()) {
            // 未正常返回，无需执行其他判断
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5. 文件存在，文件类型校验
            String contentType = httpResponse.header("Content-Type");
            // 不为空，才校验是否合法，这样校验规则相对宽松
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final Set<String> ALLOW_IMAGE_SUFFIXES_BY_URL = Set.of("image/jpg", "image/jpeg",
                        "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_IMAGE_SUFFIXES_BY_URL.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "不支持的图片格式：" + contentType);
            }
            // 6. 文件存在，文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isBlank(contentLengthStr) || !NumberUtil.isNumber(contentLengthStr)) {
                return;
            }
            long contentLength = Long.parseLong(contentLengthStr);
            final long ONE_M = 1024 * 1024;
            ThrowUtils.throwIf(contentLength > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}