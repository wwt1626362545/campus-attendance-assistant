package com.educheck.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 设置缓存 */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 设置缓存带过期时间 */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /** 获取缓存 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? (T) val : null;
    }

    /** 获取缓存（无类型转换） */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 删除缓存 */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /** 判断 key 是否存在 */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 设置过期时间 */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /** 自增（用于计数器、限流） */
    public long increment(String key) {
        Long val = redisTemplate.opsForValue().increment(key);
        return val != null ? val : 0;
    }

    /** 自增并设置过期时间 */
    public long increment(String key, long timeout, TimeUnit unit) {
        Long val = redisTemplate.opsForValue().increment(key);
        if (val != null && val == 1) {
            redisTemplate.expire(key, timeout, unit);
        }
        return val != null ? val : 0;
    }

    /** SETNX 分布式锁（尝试加锁） */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    /** 释放分布式锁 */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /** 添加 Set 元素 */
    public void addToSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    /** 判断 Set 是否包含元素 */
    public boolean isMemberOfSet(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }
}
