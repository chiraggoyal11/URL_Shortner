# Scalable URL Shortener -- Complete System Design & Implementation Guide

## 1. Project Overview

A Scalable URL Shortener converts long URLs into short, unique links
that redirect users efficiently. Example:

Long URL:
https://www.example.com/products/category/electronics/mobile/some-very-long-id

Short URL: https://yourapp.com/aZ91xK

This project demonstrates: - Backend architecture - Database design -
Caching strategies - Scalability principles - Clean code structure -
Production-level thinking

Tech Stack: - Java 17 - Spring Boot 3+ - Spring Data JPA - PostgreSQL -
Redis - Maven - Docker - JUnit + Mockito

------------------------------------------------------------------------

## 2. Functional Requirements

### 2.1 Create Short URL

POST /api/v1/urls

Request Body: { "originalUrl": "https://example.com", "customAlias":
"optional", "expiryDate": "optional" }

Process: 1. Validate URL format. 2. If custom alias exists, verify
uniqueness. 3. Save URL entity to DB. 4. Generate Base62 short code from
ID. 5. Store mapping in Redis. 6. Return short URL.

------------------------------------------------------------------------

### 2.2 Redirect Short URL

GET /{shortCode}

Process: 1. Check Redis cache. 2. If not found, query database. 3. If
expired → return 410. 4. If not found → return 404. 5. Increment click
count asynchronously. 6. Return HTTP 302 redirect.

------------------------------------------------------------------------

## 3. Database Design

  Column         Type          Constraints
  -------------- ------------- ---------------
  id             BIGSERIAL     PRIMARY KEY
  original_url   TEXT          NOT NULL
  short_code     VARCHAR(10)   UNIQUE, INDEX
  created_at     TIMESTAMP     NOT NULL
  expiry_date    TIMESTAMP     NULL
  click_count    BIGINT        DEFAULT 0

  : urls

Indexes: - Index on short_code - Unique constraint on short_code

------------------------------------------------------------------------

## 4. Short Code Generation

### Recommended Approach: Auto-Increment + Base62 Encoding

Steps: 1. Save entity → get auto-generated ID. 2. Convert ID to Base62.
3. Update record with short code.

Base62 Characters: a-z, A-Z, 0-9

Benefits: - Short URLs - URL safe - Collision free (if based on unique
ID)

------------------------------------------------------------------------

## 5. Caching Strategy (Redis)

Why? - Avoid DB hits on every redirect. - Improve response time.

Implementation: - Cache key: shortCode - Cache value: originalUrl - TTL:
24 hours

Flow: Create → Save in DB + Redis\
Redirect → Check Redis → Fallback to DB

------------------------------------------------------------------------

## 6. Asynchronous Analytics

Click count increment should not block redirect.

Use: - @Async annotation OR - ExecutorService

Advanced option: - Push events to queue (Kafka/RabbitMQ) - Batch update
DB

------------------------------------------------------------------------

## 7. Project Structure

com.urlshortener ├── controller ├── service ├── repository ├── model ├──
dto ├── config ├── exception ├── util

Follow: - SOLID principles - DTO pattern - Global exception handling

------------------------------------------------------------------------

## 8. Non-Functional Requirements

-   Stateless application
-   Proper HTTP status codes
-   Logging using SLF4J
-   Input validation
-   Docker support
-   Unit testing coverage

------------------------------------------------------------------------

## 9. Scalability Strategy

### Horizontal Scaling

-   Multiple app instances
-   Load balancer
-   Stateless design

### Database Scaling

-   Read replicas
-   Partitioning
-   Index optimization

### Caching Scaling

-   Redis cluster

### High Traffic Handling

-   Async logging
-   Rate limiting
-   CDN (optional)

------------------------------------------------------------------------

## 10. CAP Theorem Consideration

URL shortener prioritizes: - Availability - Partition tolerance

Eventual consistency acceptable for analytics.

------------------------------------------------------------------------

## 11. Docker Setup

Services: - Spring Boot App - PostgreSQL - Redis

Use docker-compose to orchestrate services.

------------------------------------------------------------------------

## 12. Testing Strategy

Unit Tests: - Service layer - Base62 encoder - Validation logic

Integration Tests: - Controller endpoints - Redis integration

------------------------------------------------------------------------

## 13. Deployment Strategy

Options: - AWS EC2 - AWS Elastic Beanstalk - Render - Railway

Add: - Environment variables - Health check endpoint - Logging
configuration

------------------------------------------------------------------------

## 14. Advanced Enhancements

-   Custom alias support
-   Expiry links
-   QR code generation
-   Rate limiting per IP
-   User accounts
-   Pagination
-   Soft delete

------------------------------------------------------------------------

## 15. Interview Preparation Points

Be ready to answer: - Why Redis? - How does indexing improve
performance? - How to scale to 1M users? - How to prevent race
conditions? - How to handle DB bottlenecks? - Trade-offs in ID
generation? - What if cache crashes?

------------------------------------------------------------------------

## 16. Resume Description Example

Designed and deployed a scalable URL shortener using Spring Boot and
Redis, implementing Base62 encoding and async analytics. Optimized
performance using caching and DB indexing, reducing response latency by
40%.

------------------------------------------------------------------------

## 17. Conclusion

This project demonstrates: - Backend engineering skills - System design
thinking - Performance optimization - Scalability knowledge - Production
readiness

Perfect for Amazon SDE-1 level interviews.
