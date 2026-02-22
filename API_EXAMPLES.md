# URL Shortener - API Usage Examples

## Using cURL

### 1. Create a Short URL (Simple)
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com/very/long/url/that/needs/shortening"
  }'
```

### 2. Create a Short URL with Custom Alias
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.github.com/yourrepo",
    "customAlias": "github"
  }'
```

### 3. Create a Short URL with Expiry Date
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.event.com/tickets",
    "expiryDate": "2026-12-31T23:59:59"
  }'
```

### 4. Redirect (Test in Browser or cURL)
```bash
# This will show you the redirect
curl -L http://localhost:8080/abc123

# To see redirect headers
curl -I http://localhost:8080/abc123
```

### 5. Get URL Statistics
```bash
curl http://localhost:8080/api/v1/urls/abc123/stats
```

### 6. Health Check
```bash
curl http://localhost:8080/actuator/health
```

---

## Using Postman

### Create Short URL
1. Method: **POST**
2. URL: `http://localhost:8080/api/v1/urls`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "originalUrl": "https://www.example.com",
  "customAlias": "example"
}
```

### Test Redirect
1. Method: **GET**
2. URL: `http://localhost:8080/example`
3. Postman will automatically follow the redirect

---

## Testing Scenarios

### Test 1: Basic Flow
```bash
# Create short URL
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.google.com"}')

# Extract short code
SHORT_CODE=$(echo $RESPONSE | jq -r '.shortCode')

# Test redirect
curl -I http://localhost:8080/$SHORT_CODE

# Get stats
curl http://localhost:8080/api/v1/urls/$SHORT_CODE/stats
```

### Test 2: Error Handling
```bash
# Invalid URL format
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "not-a-url"}'

# Duplicate custom alias
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com", "customAlias": "test"}'

# Try again with same alias (should fail)
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://another.com", "customAlias": "test"}'

# Non-existent short code
curl -I http://localhost:8080/nonexistent
```

### Test 3: Cache Performance
```bash
# Create URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.example.com", "customAlias": "cached"}'

# First access (cache miss - check logs)
time curl -I http://localhost:8080/cached

# Second access (cache hit - should be faster)
time curl -I http://localhost:8080/cached
```

---

## Load Testing with Apache Bench

```bash
# Test redirect performance
ab -n 1000 -c 10 http://localhost:8080/your-short-code

# Test create endpoint
ab -n 100 -c 5 -p payload.json -T application/json \
  http://localhost:8080/api/v1/urls
```

Where `payload.json`:
```json
{"originalUrl": "https://www.example.com"}
```

---

## Docker Testing

```bash
# Start all services
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Test from host
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.docker-test.com"}'

# Check logs
docker-compose logs -f app

# Stop services
docker-compose down
```

---

## Database Verification

```bash
# Connect to PostgreSQL
docker exec -it url-shortener-postgres psql -U postgres -d urlshortener

# Check created URLs
SELECT * FROM urls;

# Check specific short code
SELECT * FROM urls WHERE short_code = 'abc123';

# Exit
\q
```

---

## Redis Cache Verification

```bash
# Connect to Redis
docker exec -it url-shortener-redis redis-cli

# Check cached URLs
KEYS url:*

# Get specific cached URL
GET url:abc123

# Check TTL
TTL url:abc123

# Exit
exit
```
