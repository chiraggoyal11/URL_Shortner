package com.urlshortener.ratelimit;

import com.urlshortener.monitoring.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;
    private final MetricsService metricsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Determine rate limit based on endpoint
        int limit;
        Duration duration;

        if (requestUri.startsWith("/api/v1/urls") && "POST".equals(method)) {
            limit = rateLimitConfig.getCreateUrl().getLimit();
            duration = rateLimitConfig.getCreateUrl().getDuration();
        } else if (requestUri.matches("^/[a-zA-Z0-9]+$") && "GET".equals(method)) {
            limit = rateLimitConfig.getRedirect().getLimit();
            duration = rateLimitConfig.getRedirect().getDuration();
        } else if (requestUri.contains("/stats")) {
            limit = rateLimitConfig.getStats().getLimit();
            duration = rateLimitConfig.getStats().getDuration();
        } else {
            // No rate limit for other endpoints
            return true;
        }

        String rateLimitKey = clientIp + ":" + requestUri;

        if (!rateLimitService.isAllowed(rateLimitKey, limit, duration)) {
            metricsService.incrementRateLimitExceeded();
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"status\": 429, \"error\": \"TOO_MANY_REQUESTS\", \"message\": \"Rate limit exceeded. Try again later.\", \"timestamp\": %d}",
                System.currentTimeMillis()
            ));
            return false;
        }

        // Add rate limit headers
        long remaining = rateLimitService.getRemainingRequests(rateLimitKey, limit);
        response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
