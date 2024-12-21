package com.lwh.pictureproject.manager;

import com.lwh.pictureproject.config.CosClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
