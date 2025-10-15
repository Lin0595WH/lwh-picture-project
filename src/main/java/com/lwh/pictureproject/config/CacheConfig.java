package com.lwh.pictureproject.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author Lin
 * @Date 2025/10/13 21:37
 * @Descriptions 开启缓存配置类
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PICTURE_TAG_CATEGORY = "lin-picture:tag_category";

    private Duration getRandomTtl(int baseSeconds, int rangeSeconds) {
        int randomSeconds = baseSeconds + ThreadLocalRandom.current().nextInt(rangeSeconds);
        return Duration.ofSeconds(randomSeconds);
    }

    /**
     * Caffeine 本地缓存管理器
     * 用于高频访问数据的快速读取
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 设置默认配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(this.getRandomTtl(300, 120))
                .initialCapacity(100)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }

    /**
     * Redis 分布式缓存管理器
     * 用于数据共享和持久化缓存
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(this.getRandomTtl(600, 600)) // Redis缓存10-20分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        // 为特定缓存设置独立配置
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        configMap.put(CACHE_PICTURE_TAG_CATEGORY,
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(this.getRandomTtl(1800, 1800)) // 图片分类缓存30-60分钟（数据相对稳定）
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                        .disableCachingNullValues());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configMap)
                .build();
    }

    /**
     * 组合缓存管理器 - 设为默认
     * 查询顺序：Caffeine -> Redis -> DB
     */
    @Primary
    @Bean
    public CacheManager compositeCacheManager(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
                caffeineCacheManager,
                redisCacheManager
        );
        // 重要配置：
        compositeCacheManager.setFallbackToNoOpCache(false); // 不使用无操作缓存
        return compositeCacheManager;
    }
}
