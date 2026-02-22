package com.urlshortener.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Snowflake ID Generator for distributed systems
 * 
 * 64-bit ID structure:
 * - 1 bit: unused (always 0)
 * - 41 bits: timestamp in milliseconds
 * - 10 bits: machine ID (datacenter + worker)
 * - 12 bits: sequence number
 * 
 * Generates 4096 unique IDs per millisecond per machine
 */
@Component
@Slf4j
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00 UTC
    
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long workerId;
    
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${snowflake.datacenter-id:0}") long datacenterId,
            @Value("${snowflake.worker-id:0}") long workerId) {
        
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("Datacenter ID must be between 0 and %d", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
        }
        
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        
        log.info("Snowflake ID Generator initialized with datacenter={}, worker={}", 
                datacenterId, workerId);
    }

    public synchronized long nextId() {
        long timestamp = currentTime();

        // Clock moved backwards
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // Wait for clock to catch up
                try {
                    wait(offset << 1);
                    timestamp = currentTime();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while waiting for clock sync");
                }
            } else {
                throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
            }
        }

        // Same millisecond
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted, wait for next millisecond
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getWorkerId() {
        return workerId;
    }
}
