package com.urlshortener.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Check if request is allowed based on rate limit
     * @param key Unique identifier (e.g., IP address)
     * @param limit Number of requests allowed
     * @param duration Time window
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int limit, Duration duration) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        
        try {
            // Get current count
            String countStr = redisTemplate.opsForValue().get(redisKey);
            long currentCount = countStr != null ? Long.parseLong(countStr) : 0;
            
            if (currentCount >= limit) {
                log.warn("Rate limit exceeded for key: {}", key);
                return false;
            }
            
            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(redisKey);
            
            // Set expiry on first request
            if (newCount == 1) {
                redisTemplate.expire(redisKey, duration);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fail open - allow request if Redis fails
            return true;
        }
    }

    /**
     * Get remaining requests for a key
     */
    public long getRemainingRequests(String key, int limit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String countStr = redisTemplate.opsForValue().get(redisKey);
        long currentCount = countStr != null ? Long.parseLong(countStr) : 0;
        return Math.max(0, limit - currentCount);
    }

    /**
     * Reset rate limit for a key
     */
    public void reset(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        redisTemplate.delete(redisKey);
    }
}
