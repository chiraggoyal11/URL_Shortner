package com.urlshortener.analytics;

import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final UrlRepository urlRepository;

    @KafkaListener(topics = "url-clicks", groupId = "url-shortener-analytics")
    @Transactional
    public void consumeClickEvent(ClickEvent event) {
        try {
            log.debug("Processing click event for short code: {}", event.getShortCode());
            
            urlRepository.findByShortCode(event.getShortCode())
                    .ifPresent(url -> {
                        urlRepository.incrementClickCount(url.getId());
                        log.debug("Incremented click count for: {}", event.getShortCode());
                    });
        } catch (Exception e) {
            log.error("Error processing click event for short code: {}", event.getShortCode(), e);
        }
    }
}
