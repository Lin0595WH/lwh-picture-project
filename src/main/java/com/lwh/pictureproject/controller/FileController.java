package com.lwh.pictureproject.controller;

import com.lwh.pictureproject.annotation.AuthCheck;
import com.lwh.pictureproject.common.BaseResponse;
import com.lwh.pictureproject.common.ResultUtils;
import com.lwh.pictureproject.constant.UserConstant;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import com.lwh.pictureproject.manager.CosManager;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author Lin
 * @version 1.0.0
 * @description 文件上传接口
 * @date 2024/12/21 23:26
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile 文件
     * @return 文件路径
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(filePath, null);
            // 将multipartFile写入临时文件
            multipartFile.transferTo(file);
            // 上传文件到对象存储          
            cosManager.putObject(filePath, file);
            // 返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            FileController.log.error("file upload error, filepath = " + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    FileController.log.error("file delete error, filepath = {}", filePath);
                }
            }
        }
    }


    /**
     * @param filepath 文件路径
     * @param response 响应对象
     * @description: 测试文件下载
     * @author: Lin
     * @date: 2024/12/21 23:44
     * @return: void
     **/
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) {
        try (COSObjectInputStream cosObjectInput = cosManager.getObject(filepath).getObjectContent()) {
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            FileController.log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }
}
