# API Testing Guide

## 📚 Overview

This guide provides comprehensive testing instructions for the Data Streaming Application APIs, including sample requests, expected responses, and testing scenarios.

## 🎯 Testing Prerequisites

### 1. Application Running

Ensure the application is running:

```bash
./start-app.sh
# OR
docker-compose up -d --build
```

### 2. Health Check

Verify all services are healthy:

```bash
curl http://localhost:8081/actuator/health  # Order Producer
curl http://localhost:8083/actuator/health  # Analytics Consumer
```

### 3. Required Tools

- cURL (command line testing)
- Postman (GUI testing)
- Browser (for dashboard access)

## 🔄 Order Producer API Testing

### Base URL: http://localhost:8081

### Health Check

```bash
curl -X GET http://localhost:8081/actuator/health
```

**Expected Response:**

```json
{
  "status": "UP",
  "components": {
    "kafka": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Create Normal Order

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-001",
    "customerId": "customer-123",
    "amount": 299.99,
    "timestamp": '$(date +%s000)',
    "items": ["laptop", "wireless-mouse", "keyboard"]
  }'
```

**Expected Response:**

```
Order received and published
```

### Create High-Value Order (Fraud Alert Trigger)

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-002",
    "customerId": "customer-456",
    "amount": 1500.00,
    "timestamp": '$(date +%s000)',
    "items": ["expensive-laptop", "premium-software"]
  }'
```

### Create Multiple Rapid Orders (Fraud Alert Trigger)

```bash
# Send 6 orders quickly for same customer
for i in {1..6}; do
  curl -X POST http://localhost:8081/orders \
    -H "Content-Type: application/json" \
    -d '{
      "orderId": "rapid-'$i'",
      "customerId": "customer-rapid",
      "amount": 99.99,
      "timestamp": '$(date +%s000)',
      "items": ["item-'$i'"]
    }'
  sleep 0.1
done
```

### Invalid Order Testing

```bash
# Missing required field
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-789",
    "amount": 150.00
  }'

# Invalid amount (negative)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-invalid",
    "customerId": "customer-789",
    "amount": -50.00,
    "timestamp": '$(date +%s000)',
    "items": ["refund-item"]
  }'
```

**Expected Response:**

```json
{
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["orderId is required", "amount must be positive"]
}
```

## 📊 Analytics Consumer API Testing

### Base URL: http://localhost:8083

### Health Check

```bash
curl -X GET http://localhost:8083/actuator/health
```

### Get Hourly Sales Analytics

```bash
curl -X GET http://localhost:8083/analytics/sales
```

**Expected Response:**

```json
[
  {
    "id": 1,
    "windowStart": "2024-06-01T10:00:00",
    "totalSales": 1849.97,
    "averageOrderValue": 462.49
  }
]
```

### Get Customer Statistics

```bash
curl -X GET http://localhost:8083/analytics/customers
```

**Expected Response:**

```json
[
  {
    "id": 1,
    "windowStart": "2024-06-01T10:00:00",
    "customerId": "customer-123",
    "orders": 1,
    "totalSpent": 299.99
  },
  {
    "id": 2,
    "windowStart": "2024-06-01T10:00:00",
    "customerId": "customer-456",
    "orders": 1,
    "totalSpent": 1500.0
  }
]
```

### Get Fraud Alerts

```bash
curl -X GET http://localhost:8083/analytics/fraud
```

**Expected Response:**

```json
[
  {
    "id": 1,
    "customerId": "customer-456",
    "reason": "Order amount > $1000",
    "orderId": "order-002",
    "amount": 1500.0,
    "orderCount": null
  },
  {
    "id": 2,
    "customerId": "customer-rapid",
    "reason": ">5 orders per minute",
    "orderId": null,
    "amount": null,
    "orderCount": 6
  }
]
```

## 🖥️ Infrastructure Monitoring

### Kafka UI

```bash
# Open in browser
open http://localhost:8080
```

**What to Check:**

- Topics: `orders`, `analytics`
- Messages in topics
- Consumer groups
- Broker health

### Flink Dashboard

```bash
# Open in browser
open http://localhost:8082
```

**What to Check:**

- Running jobs status
- Task managers
- Job metrics
- Processing throughput

### Flink Jobs API

```bash
# List all jobs
curl http://localhost:8082/jobs

# Get job details
curl http://localhost:8082/jobs/{job-id}

# Get task managers
curl http://localhost:8082/taskmanagers
```

## 🧪 End-to-End Testing Scenarios

### Scenario 1: Normal Order Flow

```bash
# 1. Create order
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "e2e-001",
    "customerId": "e2e-customer",
    "amount": 450.00,
    "timestamp": '$(date +%s000)',
    "items": ["monitor", "webcam"]
  }'

# 2. Wait for processing (30 seconds)
sleep 30

# 3. Check analytics
curl http://localhost:8083/analytics/sales
curl http://localhost:8083/analytics/customers
```

### Scenario 2: Fraud Detection Flow

```bash
# 1. Create high-value order
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "fraud-001",
    "customerId": "fraud-customer",
    "amount": 2000.00,
    "timestamp": '$(date +%s000)',
    "items": ["expensive-item"]
  }'

# 2. Wait for processing
sleep 10

# 3. Check fraud alerts
curl http://localhost:8083/analytics/fraud
```

### Scenario 3: High Volume Testing

```bash
# Generate 100 orders
for i in {1..100}; do
  curl -X POST http://localhost:8081/orders \
    -H "Content-Type: application/json" \
    -d '{
      "orderId": "load-'$i'",
      "customerId": "customer-'$((i % 10))'",
      "amount": '$((RANDOM % 500 + 50))',
      "timestamp": '$(date +%s000)',
      "items": ["item-'$i'"]
    }' &

  # Control concurrency
  if (( i % 10 == 0 )); then
    wait
  fi
done
wait

# Check system performance
curl http://localhost:8082/jobs
curl http://localhost:8083/analytics/sales
```

## 🔍 Debugging & Troubleshooting

### Check Service Logs

```bash
# Order Producer logs
docker-compose logs order-producer

# Stream Processor logs
docker-compose logs stream-processor

# Analytics Consumer logs
docker-compose logs analytics-consumer

# Follow logs in real-time
docker-compose logs -f analytics-consumer
```

### Verify Kafka Topics

```bash
# List topics
docker exec datastreamingapp-kafka-1 kafka-topics --bootstrap-server localhost:29092 --list

# Check messages in orders topic
docker exec datastreamingapp-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic orders \
  --from-beginning \
  --timeout-ms 5000

# Check messages in analytics topic
docker exec datastreamingapp-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic analytics \
  --from-beginning \
  --timeout-ms 5000
```

### Database Verification

```bash
# Connect to PostgreSQL
docker exec -it datastreamingapp-postgres-1 psql -U analytics -d analytics

# SQL queries
SELECT COUNT(*) FROM hourly_sales;
SELECT COUNT(*) FROM customer_stats;
SELECT COUNT(*) FROM fraud_alerts;
SELECT * FROM fraud_alerts ORDER BY id DESC LIMIT 5;
```

## 📊 Performance Testing

### Throughput Testing

```bash
# Test order creation throughput
time for i in {1..1000}; do
  curl -s -X POST http://localhost:8081/orders \
    -H "Content-Type: application/json" \
    -d '{
      "orderId": "perf-'$i'",
      "customerId": "perf-customer",
      "amount": 100.00,
      "timestamp": '$(date +%s000)',
      "items": ["test-item"]
    }' > /dev/null &

  if (( i % 50 == 0 )); then
    wait
  fi
done
wait
```

### Latency Testing

```bash
# Test end-to-end latency
start_time=$(date +%s%N)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "latency-test",
    "customerId": "latency-customer",
    "amount": 200.00,
    "timestamp": '$(date +%s000)',
    "items": ["latency-item"]
  }'

# Wait and check if processed
while true; do
  response=$(curl -s http://localhost:8083/analytics/customers | grep "latency-customer")
  if [[ ! -z "$response" ]]; then
    end_time=$(date +%s%N)
    latency=$(( (end_time - start_time) / 1000000 ))
    echo "End-to-end latency: ${latency}ms"
    break
  fi
  sleep 1
done
```

## 📝 Test Results Documentation

### Expected Performance Metrics

- **Order ingestion**: < 10ms response time
- **End-to-end processing**: < 60 seconds for analytics
- **Fraud detection**: < 5 seconds for alerts
- **Throughput**: > 1000 orders/minute

### Success Criteria

- ✅ All health checks return UP
- ✅ Orders are successfully published to Kafka
- ✅ Analytics are computed and stored
- ✅ Fraud alerts are generated for suspicious activity
- ✅ No data loss during processing
- ✅ System remains stable under load

## 🚀 Postman Collection Usage

1. **Import Collection**: Import `docs/postman_collection.json`
2. **Set Environment**: Create environment with `baseUrl = http://localhost`
3. **Run Collection**: Execute all requests in sequence
4. **Automated Testing**: Use Postman's test scripts for assertions

### Sample Postman Test Script

```javascript
// Test for successful order creation
pm.test("Order created successfully", function () {
  pm.response.to.have.status(200);
  pm.response.to.have.body("Order received and published");
});

// Test for analytics data presence
pm.test("Analytics data exists", function () {
  const responseJson = pm.response.json();
  pm.expect(responseJson).to.be.an("array");
  pm.expect(responseJson.length).to.be.greaterThan(0);
});
```

---

**Note**: Always ensure the application is running and healthy before executing tests. Allow sufficient time for stream processing between order creation and analytics verification.
