package com.example.tour.common.config;

import com.example.tour.common.constant.CacheNames;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache bằng Redis cho dữ liệu ĐỌC NHIỀU - ÍT ĐỔI.
 * - Giá trị lưu dạng JSON (đọc được trong Redis Insight), có kèm tên class để đọc lại đúng kiểu.
 * - Mỗi vùng cache có thời hạn (TTL) riêng; admin sửa dữ liệu thì @CacheEvict xoá ngay.
 * - Redis sập → KHÔNG làm hỏng web: lỗi cache chỉ ghi log, request vẫn đọc thẳng DB.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    /** Cấu hình chung: JSON + mặc định sống 10 phút + không cache giá trị null */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())                         // LocalDate, LocalDateTime
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Ghi tên class vào JSON để đọc lại đúng kiểu; chỉ cho phép class của dự án + java.* (an toàn khi đọc)
        mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.example.tour.")
                        .allowIfSubType("java.")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("viettrip::")
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)));
    }

    /**
     * TTL riêng từng vùng: danh mục / điểm đến gần như không đổi → lâu; chi tiết tour → 15 phút.
     * transactionAware: lệnh lưu / xoá cache CHỜ transaction commit xong mới chạy
     * → không có cảnh cache bị xoá trước commit rồi 1 request khác kịp nạp lại dữ liệu CŨ vào cache.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtls(RedisCacheConfiguration base) {
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(CacheNames.CATEGORIES, base.entryTtl(Duration.ofHours(6)))
                .withCacheConfiguration(CacheNames.LOCATIONS, base.entryTtl(Duration.ofHours(6)))
                .withCacheConfiguration(CacheNames.TOUR_DETAIL, base.entryTtl(Duration.ofMinutes(15)));
    }

    /** Redis không kết nối được → bỏ qua cache, chạy tiếp như chưa có cache */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object key) { warn("đọc", c, e); }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object key, Object v) { warn("ghi", c, e); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object key) { warn("xoá", c, e); }
            @Override public void handleCacheClearError(RuntimeException e, Cache c) { warn("xoá hết", c, e); }
        };
    }

    private static void warn(String action, Cache cache, RuntimeException e) {
        log.warn("Lỗi {} cache '{}' (bỏ qua, đọc DB): {}", action, cache.getName(), e.getMessage());
    }
}
