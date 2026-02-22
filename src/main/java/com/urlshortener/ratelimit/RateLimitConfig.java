package com.urlshortener.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitConfig {

    private EndpointLimit createUrl = new EndpointLimit(10, Duration.ofMinutes(1));
    private EndpointLimit redirect = new EndpointLimit(100, Duration.ofMinutes(1));
    private EndpointLimit stats = new EndpointLimit(30, Duration.ofMinutes(1));

    @Data
    public static class EndpointLimit {
        private int limit;
        private Duration duration;

        public EndpointLimit() {}

        public EndpointLimit(int limit, Duration duration) {
            this.limit = limit;
            this.duration = duration;
        }
    }
}
