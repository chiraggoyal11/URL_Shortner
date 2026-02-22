package com.urlshortener.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProducer {

    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private static final String TOPIC = "url-clicks";

    public void publishClickEvent(ClickEvent event) {
        try {
            CompletableFuture<SendResult<String, ClickEvent>> future = 
                    kafkaTemplate.send(TOPIC, event.getShortCode(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Click event published for short code: {}", event.getShortCode());
                } else {
                    log.error("Failed to publish click event", ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing click event", e);
        }
    }
}
