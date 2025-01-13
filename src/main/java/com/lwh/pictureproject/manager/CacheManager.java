package com.lwh.pictureproject.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lwh.pictureproject.exception.BusinessException;
import com.lwh.pictureproject.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author Lin
 * @version 1.0.0
 * @description 本地缓存调用封装类
 * @date 2025/1/6 21:54
 */
@Slf4j
@Component
public class CacheManager {
    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(Integer.getInteger("cache.initialCapacity", 1024))
            .maximumSize(Long.getLong("cache.maximumSize", 10_000L)) // 最大 10000 条
            .expireAfterWrite(Duration.ofMinutes(Integer.getInteger("cache.expireAfterWriteMinutes", 5)))
            .build();

    public CacheManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 显式关闭缓存，确保资源释放
                LOCAL_CACHE.invalidateAll();
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "缓存关闭异常：" + e);
            }
        }));
    }

    /**
     * 获取缓存
     *
     * @param key 缓存的key
     * @return
     */
    public String getFromCache(String key) {
        try {
            return LOCAL_CACHE.getIfPresent(key);
        } catch (Exception e) {
            log.error("Error accessing cache: {}", e);
            return null;
        }
    }

    /**
     * 添加缓存
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    public void putToCache(String key, String value) {
        try {
            LOCAL_CACHE.put(key, value);
        } catch (Exception e) {
            log.error("Error putting value into cache: {}", e);
        }
    }


    /**
     * @description: 清除缓存
     * @author: Lin
     * @date: 2025/1/7 22:25
     * @return: void
     **/
    public void removeCache() {
        try {
            LOCAL_CACHE.invalidateAll();
        } catch (Exception e) {
            log.error("Error removing value from cache: {}", e);
        }
    }

}
