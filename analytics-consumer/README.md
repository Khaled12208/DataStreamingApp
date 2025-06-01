# Analytics Consumer Service

## Overview

The Analytics Consumer is a Spring Boot microservice that consumes processed analytics events from Kafka, stores them in PostgreSQL database, and exposes REST APIs for querying analytics data.

## 🏗️ Architecture

- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: PostgreSQL 15
- **Message Broker**: Apache Kafka
- **ORM**: Spring Data JPA (Hibernate)
- **Build Tool**: Maven

## 🔄 Data Flow

1. **Consume** → Listen to Kafka `analytics` topic
2. **Deserialize** → Parse JSON analytics events
3. **Transform** → Convert to JPA entities
4. **Persist** → Store in PostgreSQL database
5. **Expose** → REST APIs for data access

## 📊 Data Models

### Hourly Sales

- **Purpose**: Aggregated sales metrics per hour
- **Table**: `hourly_sales`
- **Fields**:
  - `id`: Auto-generated primary key
  - `window_start`: Hour window start time
  - `total_sales`: Sum of all order amounts
  - `average_order_value`: Average amount per order

### Customer Statistics

- **Purpose**: Customer behavior metrics per hour
- **Table**: `customer_stats`
- **Fields**:
  - `id`: Auto-generated primary key
  - `window_start`: Hour window start time
  - `customer_id`: Customer identifier
  - `orders`: Number of orders placed
  - `total_spent`: Total amount spent

### Fraud Alerts

- **Purpose**: Real-time fraud detection alerts
- **Table**: `fraud_alerts`
- **Fields**:
  - `id`: Auto-generated primary key
  - `customer_id`: Customer identifier
  - `reason`: Fraud detection reason
  - `order_id`: Related order ID (optional)
  - `amount`: Order amount (optional)
  - `order_count`: Rapid order count (optional)

## 📡 API Endpoints

### Health Check

```http
GET /actuator/health
```

**Response**: Service health and database connectivity

### Hourly Sales Analytics

```http
GET /analytics/sales
```

**Response**: List of hourly sales aggregations

```json
[
  {
    "id": 1,
    "windowStart": "2024-06-01T10:00:00",
    "totalSales": 1250.5,
    "averageOrderValue": 312.63
  }
]
```

### Customer Statistics

```http
GET /analytics/customers
```

**Response**: List of customer analytics

```json
[
  {
    "id": 1,
    "windowStart": "2024-06-01T10:00:00",
    "customerId": "cust-123",
    "orders": 3,
    "totalSpent": 450.75
  }
]
```

### Fraud Alerts

```http
GET /analytics/fraud
```

**Response**: List of fraud detection alerts

```json
[
  {
    "id": 1,
    "customerId": "cust-456",
    "reason": "Order amount > $1000",
    "orderId": "order-789",
    "amount": 1500.0,
    "orderCount": null
  }
]
```

## ⚙️ Configuration

### Database Configuration

- **Driver**: PostgreSQL JDBC Driver
- **Connection Pool**: HikariCP
- **DDL**: Auto-update schema on startup
- **Transaction**: Auto-commit disabled

### Kafka Configuration

- **Bootstrap Servers**: kafka:9092
- **Consumer Group**: analytics-consumer
- **Topic**: `analytics`
- **Auto Offset Reset**: earliest
- **Concurrency**: 1 consumer thread

### Environment Variables

- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `SPRING_PROFILES_ACTIVE`: Active Spring profile

## 🚀 Running Locally

### Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 15+
- Running Kafka cluster with analytics topic

### Database Setup

```sql
-- Create database
CREATE DATABASE analytics;

-- Create user (optional)
CREATE USER analytics WITH PASSWORD 'analytics';
GRANT ALL PRIVILEGES ON DATABASE analytics TO analytics;
```

### Build & Run

```bash
# Build
mvn clean package

# Run locally (with local database)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/analytics
export SPRING_DATASOURCE_USERNAME=analytics
export SPRING_DATASOURCE_PASSWORD=analytics
java -jar target/analytics-consumer-1.0.0-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

### Docker

```bash
# Build image
docker build -t analytics-consumer .

# Run container
docker run -p 8083:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/analytics \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  analytics-consumer
```

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
# Requires test containers
mvn verify
```

### Manual Testing

```bash
# Health check
curl http://localhost:8083/actuator/health

# Check analytics data
curl http://localhost:8083/analytics/sales
curl http://localhost:8083/analytics/customers
curl http://localhost:8083/analytics/fraud

# Database verification
psql -h localhost -U analytics -d analytics -c "SELECT * FROM hourly_sales;"
```

## 📈 Monitoring

### Health Checks

- **Spring Boot Actuator**: `/actuator/health`
- **Database Connectivity**: Automatic health indicator
- **Kafka Consumer**: Consumer group monitoring

### Metrics

- **Message Processing**: Analytics events consumed per second
- **Database Performance**: Query execution times
- **Error Rates**: Failed message processing
- **Consumer Lag**: Kafka consumer offset lag

### Logging

- **Framework**: SLF4J + Logback
- **Levels**: DEBUG, INFO, WARN, ERROR
- **Appenders**: Console, File (configurable)
- **Structured**: JSON format support

## 🔒 Security

### Database Security

- **Connection Encryption**: SSL support
- **Credentials**: Environment variable injection
- **Connection Pooling**: Secure connection management

### API Security

- **Input Validation**: Automatic validation
- **SQL Injection**: Parameterized queries via JPA
- **Authentication**: Future enhancement capability

## 🚨 Error Handling

### Kafka Consumer Errors

- **Deserialization Failures**: Skip and log malformed messages
- **Processing Errors**: Retry with exponential backoff
- **Dead Letter Queue**: Future enhancement for failed messages

### Database Errors

- **Connection Failures**: Automatic reconnection
- **Constraint Violations**: Graceful error handling
- **Transaction Rollback**: Atomic operations

### Common Issues

1. **Database Connection**: Check PostgreSQL accessibility
2. **Kafka Connectivity**: Verify broker availability
3. **Schema Evolution**: Handle JSON structure changes
4. **Memory Usage**: Monitor JPA entity cache

## 🎯 Performance Optimization

### Database Optimizations

- **Indexes**: Automatic primary key indexes
- **Connection Pooling**: HikariCP configuration
- **Batch Processing**: Batch inserts for high throughput
- **Query Optimization**: JPA query hints

### Kafka Consumer Tuning

- **Batch Size**: Optimize consumer fetch size
- **Concurrency**: Scale consumer threads
- **Offset Management**: Manual vs automatic commits
- **Memory Management**: Consumer buffer sizing

## 📦 Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Kafka
- PostgreSQL JDBC Driver
- Spring Boot Actuator
- Jackson for JSON processing

## 🔮 Future Enhancements

- **Real-time Dashboards**: WebSocket streaming APIs
- **Data Visualization**: Built-in chart endpoints
- **Data Export**: CSV/Excel export capabilities
- **Analytics Aggregation**: Multi-dimensional analytics
- **Caching**: Redis integration for performance
- **Authentication**: OAuth2/JWT security integration
