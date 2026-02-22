package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.CustomAliasAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CACHE_PREFIX = "url:";
    private static final long CACHE_TTL_HOURS = 24;

    /**
     * Create a new short URL
     */
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        log.info("Creating short URL for: {}", request.getOriginalUrl());

        // Handle custom alias
        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (urlRepository.existsByShortCode(request.getCustomAlias())) {
                throw new CustomAliasAlreadyExistsException("Custom alias already exists: " + request.getCustomAlias());
            }
            shortCode = request.getCustomAlias();
        }

        // Parse expiry date
        LocalDateTime expiryDate = null;
        if (request.getExpiryDate() != null && !request.getExpiryDate().isEmpty()) {
            expiryDate = LocalDateTime.parse(request.getExpiryDate(), DateTimeFormatter.ISO_DATE_TIME);
        }

        // Build URL entity (without short code for custom alias)
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(request.getCustomAlias())
                .expiryDate(expiryDate)
                .clickCount(0L)
                .build();

        // Save to DB
        url = urlRepository.save(url);

        // Generate Base62 short code if no custom alias
        if (request.getCustomAlias() == null || request.getCustomAlias().isEmpty()) {
            shortCode = base62Encoder.encode(url.getId());
            url.setShortCode(shortCode);
            url = urlRepository.save(url);
        }

        // Cache in Redis
        String cacheKey = CACHE_PREFIX + shortCode;
        redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), CACHE_TTL_HOURS, TimeUnit.HOURS);

        log.info("Short URL created: {} -> {}", shortCode, url.getOriginalUrl());

        return buildUrlResponse(url);
    }

    /**
     * Get original URL by short code with Redis caching
     */
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        log.info("Fetching original URL for short code: {}", shortCode);

        // Try Redis cache first
        String cacheKey = CACHE_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            log.info("Cache hit for: {}", shortCode);
            // Async increment click count
            asyncIncrementClickCount(shortCode);
            return cachedUrl;
        }

        log.info("Cache miss for: {}", shortCode);

        // Fallback to database
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        // Check expiry
        if (url.isExpired()) {
            throw new UrlExpiredException("This short URL has expired");
        }

        // Update cache
        redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), CACHE_TTL_HOURS, TimeUnit.HOURS);

        // Async increment click count
        asyncIncrementClickCount(shortCode);

        return url.getOriginalUrl();
    }

    /**
     * Get URL statistics
     */
    @Transactional(readOnly = true)
    public UrlResponse getUrlStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        return buildUrlResponse(url);
    }

    /**
     * Async increment click count
     */
    @Async
    public void asyncIncrementClickCount(String shortCode) {
        try {
            Url url = urlRepository.findByShortCode(shortCode).orElse(null);
            if (url != null) {
                urlRepository.incrementClickCount(url.getId());
                log.debug("Incremented click count for: {}", shortCode);
            }
        } catch (Exception e) {
            log.error("Failed to increment click count for: {}", shortCode, e);
        }
    }

    /**
     * Build URL response DTO
     */
    private UrlResponse buildUrlResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiryDate(url.getExpiryDate())
                .clickCount(url.getClickCount())
                .build();
    }
}
