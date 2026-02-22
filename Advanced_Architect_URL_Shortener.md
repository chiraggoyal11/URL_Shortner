# Scalable URL Shortener -- Architect-Level System Design Guide

## 1. Problem Statement

Design a highly scalable URL Shortener capable of handling millions of
requests per day with high availability, low latency, and horizontal
scalability.

------------------------------------------------------------------------

# 2. Capacity Planning & Traffic Estimation

## Assumptions

-   10 million new URLs created per month
-   100 million redirects per month
-   Read-heavy system (10:1 read/write ratio)

## QPS Calculation

Writes: 10,000,000 / (30 × 24 × 3600) ≈ 4 writes/sec

Reads: 100,000,000 / (30 × 24 × 3600) ≈ 40 reads/sec

Peak Traffic Multiplier (×5): Writes ≈ 20/sec Reads ≈ 200/sec

System must comfortably handle: - 20 write QPS - 200 read QPS - Designed
for future scale to 10x

------------------------------------------------------------------------

# 3. Storage Estimation

## URL Record Size Approximation

-   ID: 8 bytes
-   Original URL: \~500 bytes avg
-   Short code: 8 bytes
-   Timestamp fields: 16 bytes
-   Click count: 8 bytes
-   Index overhead: \~50 bytes

Approx per record ≈ 600 bytes

For 10M URLs: 10,000,000 × 600 bytes ≈ 6 GB/year

5-year retention: ≈ 30 GB (manageable for relational DB)

------------------------------------------------------------------------

# 4. High-Level Architecture

Client → Load Balancer → Application Layer → Redis Cache → Database ↓
Async Analytics Processor

------------------------------------------------------------------------

# 5. API Design

## Create Short URL

POST /api/v1/urls

## Redirect

GET /{shortCode}

Returns HTTP 302 redirect.

------------------------------------------------------------------------

# 6. Database Design

Table: urls

Columns: - id (BIGSERIAL, PK) - original_url (TEXT, NOT NULL) -
short_code (VARCHAR(10), UNIQUE, INDEX) - created_at (TIMESTAMP) -
expiry_date (TIMESTAMP) - click_count (BIGINT)

Indexes: - Unique index on short_code - Optional composite index on
(created_at, short_code)

------------------------------------------------------------------------

# 7. ID Generation Strategy

## Option 1: Auto-increment + Base62 (Recommended)

Pros: - Simple - Collision-free - Short codes

Cons: - Harder to shard globally

## Option 2: Snowflake ID Generator

-   64-bit distributed unique ID
-   Supports horizontal scaling

Better for multi-region deployment.

------------------------------------------------------------------------

# 8. Caching Strategy (Redis)

Read Flow: 1. Check Redis 2. If miss → Query DB 3. Populate cache 4.
Return redirect

TTL Strategy: - 24-hour TTL - Refresh on hit (optional)

Expected cache hit rate: \> 80% for popular links

------------------------------------------------------------------------

# 9. Scaling Strategy

## Horizontal Scaling

-   Stateless app servers
-   Multiple instances behind Load Balancer

## Database Scaling

-   Primary for writes
-   Read replicas for redirects
-   Partition by short_code hash (future scale)

## Redis Scaling

-   Redis Cluster
-   Consistent hashing

------------------------------------------------------------------------

# 10. Handling 1 Billion URLs

Storage: 1B × 600 bytes ≈ 600 GB

Approach: - Shard DB by hash(short_code) - Use distributed ID
generation - Archive cold data

------------------------------------------------------------------------

# 11. Fault Tolerance

-   Multi-AZ deployment
-   DB replication
-   Redis replication
-   Circuit breaker pattern
-   Health checks

------------------------------------------------------------------------

# 12. Consistency Model (CAP Theorem)

System Preference: - Availability - Partition Tolerance

Analytics can be eventually consistent.

------------------------------------------------------------------------

# 13. Async Analytics Processing

Redirect must be \<50ms.

Approach: - Publish click event to queue - Worker consumes and batch
updates DB - Avoids write bottleneck

------------------------------------------------------------------------

# 14. Rate Limiting

Prevent abuse:

-   Token bucket algorithm
-   Per-IP rate limiting
-   Redis-based counter

------------------------------------------------------------------------

# 15. Security Considerations

-   URL validation
-   Block malicious domains
-   HTTPS only
-   Input sanitization
-   SQL injection protection

------------------------------------------------------------------------

# 16. Observability

-   Centralized logging
-   Metrics (Prometheus)
-   Dashboard (Grafana)
-   Alerting on:
    -   High latency
    -   DB errors
    -   Cache failures

------------------------------------------------------------------------

# 17. Latency Breakdown

-   Load balancer: 5ms
-   App processing: 10ms
-   Redis hit: 5ms Total redirect latency ≈ 20ms

Without cache: DB read ≈ 15--30ms Total ≈ 50ms

------------------------------------------------------------------------

# 18. Deployment Architecture

Dockerized Application CI/CD Pipeline Blue-Green Deployment

Cloud Options: - AWS EC2 - Elastic Beanstalk - Kubernetes (Advanced)

------------------------------------------------------------------------

# 19. Trade-Off Discussions

  Decision           Why
  ------------------ -----------------------
  Redis              Reduce DB load
  Base62             Short, readable codes
  Async logging      Improve latency
  Stateless design   Horizontal scaling

------------------------------------------------------------------------

# 20. Interview Deep-Dive Questions

Be ready to answer:

-   How to scale to 10B URLs?
-   How to migrate database without downtime?
-   What if Redis crashes?
-   How to handle hot keys?
-   How to design global multi-region deployment?
-   How to prevent brute-force short code scanning?

------------------------------------------------------------------------

# 21. Resume-Level Description

Architected a scalable URL shortening system designed for 100M+ monthly
redirects using Redis caching, asynchronous analytics processing, and
horizontally scalable Spring Boot services. Performed capacity planning,
storage estimation, and designed for high availability and fault
tolerance.

------------------------------------------------------------------------

# 22. Conclusion

This system demonstrates: - Real system design thinking - Capacity
planning skills - Trade-off analysis - Scalability architecture -
Production engineering mindset

This level of understanding is sufficient for Amazon SDE-1 and even
SDE-2 system design discussions.
