package com.byteBazar.catalog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration for Catalog Service
 * Configures different cache TTL settings for different cache types
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names constants
    public static final String CATEGORY_CACHE = "categories";
    public static final String PRODUCT_CACHE = "products";
    public static final String PRODUCT_SEARCH_CACHE = "product-searches";
    public static final String CATEGORY_HIERARCHY_CACHE = "category-hierarchies";
    public static final String PRODUCT_ANALYTICS_CACHE = "product-analytics";
    public static final String CATEGORY_ANALYTICS_CACHE = "category-analytics";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(10)) // Default 10 minutes TTL
                .disableCachingNullValues();

        // Cache-specific configurations with different TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Categories cache - 1 hour TTL (categories change less frequently)
        cacheConfigurations.put(CATEGORY_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofHours(1)));
        
        // Category hierarchy cache - 1 hour TTL
        cacheConfigurations.put(CATEGORY_HIERARCHY_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofHours(1)));
        
        // Products cache - 30 minutes TTL
        cacheConfigurations.put(PRODUCT_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30)));
        
        // Product search results cache - 10 minutes TTL (search results can be cached shorter)
        cacheConfigurations.put(PRODUCT_SEARCH_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofMinutes(10)));
        
        // Analytics caches - 2 hours TTL (analytics data can be cached longer)
        cacheConfigurations.put(PRODUCT_ANALYTICS_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofHours(2)));
        
        cacheConfigurations.put(CATEGORY_ANALYTICS_CACHE, defaultCacheConfig
                .entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }
}
