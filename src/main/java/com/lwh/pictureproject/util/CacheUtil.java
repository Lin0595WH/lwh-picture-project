package com.lwh.pictureproject.util;

import cn.hutool.core.collection.CollUtil;
import com.lwh.pictureproject.manager.CacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Lin
 * @version 1.0.0
 * @description 封装缓存工具类
 * @date 2025/1/6 21:58
 */
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private static StringRedisTemplate stringRedisTemplate;
    private static CacheManager cacheManager;

    @Resource
    private final StringRedisTemplate stringRedisTemplateInstance;

    @Resource
    private final CacheManager cacheManagerInstance;

    /**
     * @param key  缓存的key
     * @param type 清除的类型 0：精确按照key清除，1：模糊匹配清除
     * @description: 清除指定key的缓存
     * @author: Lin
     * @date: 2025/1/7 22:22
     * @return: void
     **/
    public static void removeCache(String key, int type) {
        // 先从本地缓存中删除，这个只能模糊匹配清除，那就暴力一点，把缓存全干掉
        cacheManager.removeCache();
        if (type == 0) {
            // 精确按照key清除
            stringRedisTemplate.delete(key);

        } else {
            // 模糊匹配清除
            Set<String> keys = stringRedisTemplate.keys(key + "*");
            if (CollUtil.isNotEmpty(keys)) {
                stringRedisTemplate.delete(keys);
            }
        }
    }

    @PostConstruct
    public void init() {
        CacheUtil.stringRedisTemplate = stringRedisTemplateInstance;
        CacheUtil.cacheManager = cacheManagerInstance;
    }

    /**
     * 从缓存中获取数据
     *
     * @param key 缓存的key
     * @return 缓存的值
     */
    public static String getFromCache(String key) {
        // 1.先从本地缓存中获取
        String cachedValue = cacheManager.getFromCache(key);
        if (cachedValue != null) {
            // 如果本地缓存中有，直接返回
            return cachedValue;
        }
        // 2.本地缓存中没有，则从Redis中获取
        cachedValue = stringRedisTemplate.opsForValue().get(key);
        // 如果Redis中有，则更新本地缓存，并且返回
        if (cachedValue != null) {
            cacheManager.putToCache(key, cachedValue);
            return cachedValue;
        }
        return null;
    }

    /**
     * 将数据放入缓存中（本地缓存）
     *
     * @param key   缓存的key
     * @param value 缓存的值
     */
    public static void putToCache(String key, String value) {
        // 更新本地缓存
        cacheManager.putToCache(key, value);
    }

    /**
     * 将数据放入缓存中（本地缓存和Redis）
     *
     * @param key           缓存的key
     * @param value         缓存的值
     * @param expireSeconds 过期时间
     */
    public static void putToCache(String key, String value, int expireSeconds) {
        // 1.先更新本地缓存
        putToCache(key, value);
        // 2.再更新Redis缓存
        stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }
}
