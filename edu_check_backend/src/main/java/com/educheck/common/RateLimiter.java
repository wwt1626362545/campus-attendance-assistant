package com.educheck.common;

import com.educheck.config.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的接口限流工具
 * 使用滑动窗口计数器实现
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final CacheService cacheService;

    /**
     * 检查是否超过限流阈值
     * @param key    限流 key（建议格式：rate:接口名:用户ID）
     * @param limit  时间窗口内允许的最大次数
     * @param window 时间窗口长度
     * @param unit   时间窗口单位
     * @return true=允许通过, false=被限流
     */
    public boolean tryAcquire(String key, int limit, long window, TimeUnit unit) {
        long count = cacheService.increment(key, window, unit);
        return count <= limit;
    }

    /**
     * 获取当前计数
     */
    public long getCurrentCount(String key) {
        Object val = cacheService.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0;
    }
}
