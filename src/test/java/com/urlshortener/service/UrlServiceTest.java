package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.CustomAliasAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testCreateShortUrl_Success() {
        // Arrange
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .build();

        Url savedUrl = Url.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("1")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(base62Encoder.encode(1L)).thenReturn("1");

        // Act
        UrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);
        assertEquals("https://example.com", response.getOriginalUrl());
        assertEquals("1", response.getShortCode());
        assertEquals("http://localhost:8080/1", response.getShortUrl());
        verify(urlRepository, times(2)).save(any(Url.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testCreateShortUrl_WithCustomAlias() {
        // Arrange
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("custom")
                .build();

        Url savedUrl = Url.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode("custom")
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlRepository.existsByShortCode("custom")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        // Act
        UrlResponse response = urlService.createShortUrl(request);

        // Assert
        assertEquals("custom", response.getShortCode());
        verify(urlRepository).existsByShortCode("custom");
    }

    @Test
    void testCreateShortUrl_CustomAliasExists() {
        // Arrange
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("custom")
                .build();

        when(urlRepository.existsByShortCode("custom")).thenReturn(true);

        // Act & Assert
        assertThrows(CustomAliasAlreadyExistsException.class, 
                () -> urlService.createShortUrl(request));
    }

    @Test
    void testGetOriginalUrl_CacheHit() {
        // Arrange
        String shortCode = "abc123";
        String originalUrl = "https://example.com";

        when(valueOperations.get("url:" + shortCode)).thenReturn(originalUrl);

        // Act
        String result = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals(originalUrl, result);
        verify(valueOperations).get("url:" + shortCode);
        verify(urlRepository, never()).findByShortCode(anyString());
    }

    @Test
    void testGetOriginalUrl_CacheMiss() {
        // Arrange
        String shortCode = "abc123";
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode(shortCode)
                .clickCount(0L)
                .build();

        when(valueOperations.get("url:" + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        // Act
        String result = urlService.getOriginalUrl(shortCode);

        // Assert
        assertEquals("https://example.com", result);
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testGetOriginalUrl_NotFound() {
        // Arrange
        String shortCode = "notfound";

        when(valueOperations.get(anyString())).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UrlNotFoundException.class, 
                () -> urlService.getOriginalUrl(shortCode));
    }

    @Test
    void testGetOriginalUrl_Expired() {
        // Arrange
        String shortCode = "expired";
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://example.com")
                .shortCode(shortCode)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        // Act & Assert
        assertThrows(UrlExpiredException.class, 
                () -> urlService.getOriginalUrl(shortCode));
    }
}
