package com.urlshortener.controller;

import com.urlshortener.analytics.AnalyticsProducer;
import com.urlshortener.analytics.ClickEvent;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final UrlService urlService;
    private final AnalyticsProducer analyticsProducer;

    /**
     * Redirect short URL to original URL
     * GET /{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        log.info("Redirecting short code: {}", shortCode);
        String originalUrl = urlService.getOriginalUrl(shortCode);
        
        // Publish analytics event to Kafka (non-blocking)
        ClickEvent event = ClickEvent.builder()
                .shortCode(shortCode)
                .timestamp(LocalDateTime.now())
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .referer(request.getHeader("Referer"))
                .build();
        analyticsProducer.publishClickEvent(event);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
