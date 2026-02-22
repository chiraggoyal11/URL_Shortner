package com.urlshortener.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {
    
    private String shortCode;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String referer;
}
