package com.urlshortener.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MetricsService {

    private final Counter urlCreationCounter;
    private final Counter redirectCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer redirectLatencyTimer;
    private final Counter rateLimitExceededCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        // URL Creation metrics
        this.urlCreationCounter = Counter.builder("url.creation.total")
                .description("Total number of URLs created")
                .register(meterRegistry);

        // Redirect metrics
        this.redirectCounter = Counter.builder("url.redirect.total")
                .description("Total number of redirects")
                .register(meterRegistry);

        // Cache metrics
        this.cacheHitCounter = Counter.builder("cache.hit.total")
                .description("Total cache hits")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("cache.miss.total")
                .description("Total cache misses")
                .register(meterRegistry);

        // Latency metrics
        this.redirectLatencyTimer = Timer.builder("redirect.latency")
                .description("Redirect latency in milliseconds")
                .register(meterRegistry);

        // Rate limit metrics
        this.rateLimitExceededCounter = Counter.builder("rate.limit.exceeded.total")
                .description("Total rate limit violations")
                .register(meterRegistry);
    }

    public void incrementUrlCreation() {
        urlCreationCounter.increment();
    }

    public void incrementRedirect() {
        redirectCounter.increment();
    }

    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordRedirectLatency(Timer.Sample sample) {
        sample.stop(redirectLatencyTimer);
    }

    public void incrementRateLimitExceeded() {
        rateLimitExceededCounter.increment();
    }
}
