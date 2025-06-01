# Order Producer Service

## Overview

The Order Producer is a Spring Boot microservice that exposes REST APIs for order creation and publishes order events to Kafka for real-time processing.

## 🏗️ Architecture

- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Message Broker**: Apache Kafka
- **Serialization**: JSON
- **Build Tool**: Maven

## 📡 API Endpoints

### Health Check

```http
GET /actuator/health
```

**Response**: Service health status

### Create Order

```http
POST /orders
Content-Type: application/json

{
  "orderId": "order-123",
  "customerId": "customer-456",
  "amount": 150.75,
  "timestamp": 1717230000000,
  "items": ["laptop", "mouse"]
}
```

**Response**:

- `200 OK`: Order received and published
- `400 Bad Request`: Invalid order data
- `500 Internal Server Error`: Processing failed

## 🔄 Data Flow

1. **Receive Order** → REST API endpoint accepts order JSON
2. **Validate** → Spring Boot validation checks required fields
3. **Serialize** → Convert order to JSON string
4. **Publish** → Send to Kafka `orders` topic
5. **Respond** → Return success/error status

## 📊 Order Schema

```json
{
  "orderId": "string (required)",
  "customerId": "string (required)",
  "amount": "number (required, > 0)",
  "timestamp": "number (required, epoch milliseconds)",
  "items": "array of strings (required, min 1 item)"
}
```

## ⚙️ Configuration

### Environment Variables

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses (default: localhost:9092)
- `SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL`: Schema Registry URL
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (docker/local)

### Kafka Configuration

- **Topic**: `orders`
- **Partitions**: 3
- **Replication Factor**: 1
- **Key**: Order ID
- **Value**: JSON serialized order

## 🚀 Running Locally

### Prerequisites

- Java 17+
- Maven 3.6+
- Running Kafka cluster

### Build & Run

```bash
# Build
mvn clean package

# Run locally
java -jar target/order-producer-1.0.0-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

### Docker

```bash
# Build image
docker build -t order-producer .

# Run container
docker run -p 8081:8081 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  order-producer
```

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Manual Testing

```bash
# Health check
curl http://localhost:8081/actuator/health

# Create order
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "test-001",
    "customerId": "cust-123",
    "amount": 199.99,
    "timestamp": 1717230000000,
    "items": ["product1", "product2"]
  }'
```

## 📈 Monitoring

### Health Checks

- **Spring Boot Actuator**: `/actuator/health`
- **Readiness**: Service can accept requests
- **Liveness**: Service is running

### Metrics

- **Request counts**: Number of orders processed
- **Response times**: API latency metrics
- **Error rates**: Failed order submissions
- **Kafka metrics**: Producer throughput, failures

### Logging

- **Level**: INFO (configurable)
- **Format**: JSON structured logging
- **Destinations**: Console, file (configurable)

## 🔒 Security Considerations

- Input validation on all fields
- Rate limiting (configurable)
- Authentication/Authorization (future enhancement)
- SSL/TLS for Kafka connections (production)

## 🚨 Error Handling

- **Validation Errors**: 400 Bad Request with details
- **Kafka Unavailable**: 503 Service Unavailable
- **Serialization Errors**: 500 Internal Server Error
- **Retry Logic**: Automatic retries for transient failures

## 📦 Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Validation
- Spring Kafka
- Jackson for JSON processing
- Spring Boot Actuator for monitoring
