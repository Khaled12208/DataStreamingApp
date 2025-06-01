# Stream Processor (Apache Flink)

## Overview

The Stream Processor is an Apache Flink application that consumes order events from Kafka, performs real-time analytics, and publishes processed results back to Kafka for downstream consumption.

## 🏗️ Architecture

- **Framework**: Apache Flink 1.18.1
- **Language**: Java 17
- **Runtime**: Flink Cluster (JobManager + TaskManager)
- **Input**: Kafka `orders` topic
- **Output**: Kafka `analytics` topic

## 🔄 Data Processing Pipeline

### Input Stream

- **Source**: Kafka topic `orders`
- **Format**: JSON messages containing order events
- **Partitioning**: 3 partitions for parallel processing
- **Watermarks**: Event-time processing for accurate windowing

### Processing Operations

#### 1. Hourly Sales Analytics

- **Window**: 1-hour tumbling windows
- **Metrics**:
  - Total sales amount
  - Average order value
  - Order count per hour
- **Output**: Aggregated sales metrics

#### 2. Customer Statistics

- **Window**: 1-hour tumbling windows per customer
- **Metrics**:
  - Number of orders per customer
  - Total amount spent per customer
  - Customer activity patterns
- **Output**: Per-customer analytics

#### 3. Fraud Detection

- **Real-time Alerts**:
  - Orders exceeding $1000 (immediate alert)
  - More than 5 orders per customer within 1 minute
- **Pattern Detection**: Suspicious activity identification
- **Output**: Fraud alert events

### Output Stream

- **Sink**: Kafka topic `analytics`
- **Format**: JSON messages with analytics results
- **Types**: `hourly_sales`, `customer_stats`, `fraud_alert`

## 📊 Event Schemas

### Input Schema (Order Event)

```json
{
  "orderId": "string",
  "customerId": "string",
  "amount": "number",
  "timestamp": "number (epoch milliseconds)",
  "items": ["string array"]
}
```

### Output Schema (Analytics Events)

#### Hourly Sales

```json
{
  "type": "hourly_sales",
  "window_start": "2024-06-01T00:00:00",
  "total_sales": 1250.5,
  "average_order_value": 312.63
}
```

#### Customer Stats

```json
{
  "type": "customer_stats",
  "window_start": "2024-06-01T00:00:00",
  "customer_id": "cust-123",
  "orders": 3,
  "total_spent": 450.75
}
```

#### Fraud Alert

```json
{
  "type": "fraud_alert",
  "customer_id": "cust-456",
  "reason": "Order amount > $1000",
  "order_id": "order-789",
  "amount": 1500.0
}
```

## ⚙️ Configuration

### Flink Job Configuration

- **Parallelism**: Auto (based on available task slots)
- **Restart Strategy**: Fixed delay (3 attempts, 10s delay)
- **Checkpointing**: Disabled (can be enabled with persistent storage)
- **State Backend**: Filesystem (for production use)

### Kafka Configuration

- **Bootstrap Servers**: kafka:9092
- **Consumer Group**: flink-stream-processor
- **Starting Offset**: Earliest
- **Serialization**: JSON strings

### Environment Variables

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `FLINK_JOBMANAGER_HOST`: Flink JobManager hostname
- `JAVA_OPTS`: JVM options for module access

## 🚀 Deployment

### Local Development

```bash
# Build
mvn clean package -DskipTests

# Submit to local Flink cluster
flink run -c com.example.streamprocessor.StreamProcessorJob \
  target/stream-processor-1.0.0-SNAPSHOT.jar
```

### Docker Deployment

```bash
# The job is automatically submitted when the container starts
docker-compose up stream-processor
```

### Flink Web UI

- **URL**: http://localhost:8082
- **Features**:
  - Job monitoring and metrics
  - Task manager status
  - Checkpoint information
  - Job history and logs

## 🔧 Development

### Adding New Analytics

1. **Create Processing Function**:

   ```java
   public class NewAnalyticsFunction extends ProcessWindowFunction<...> {
       @Override
       public void process(...) {
           // Custom analytics logic
       }
   }
   ```

2. **Add to Main Pipeline**:

   ```java
   DataStream<String> newAnalytics = orders
       .keyBy(OrderEvent::getCustomerId)
       .window(TumblingEventTimeWindows.of(Time.minutes(5)))
       .process(new NewAnalyticsFunction());
   ```

3. **Union with Output Stream**:
   ```java
   DataStream<String> analytics = hourlySales
       .union(customerStats, fraudAlerts, newAnalytics);
   ```

### Custom Serialization

- **Kryo Serializer**: Custom serialization for complex objects
- **Registration**: Registered in StreamExecutionEnvironment
- **Performance**: Optimized for Flink's internal operations

## 📈 Monitoring

### Flink Metrics

- **Throughput**: Records per second processed
- **Latency**: End-to-end processing latency
- **Backpressure**: Stream processing bottlenecks
- **Checkpoint Duration**: State persistence metrics

### Key Performance Indicators

- **Processing Rate**: Orders processed per minute
- **Alert Generation**: Fraud alerts triggered
- **Window Completeness**: On-time window processing
- **Error Rate**: Failed message processing

### Logging

- **Framework**: SLF4J + Log4j2
- **Levels**: DEBUG, INFO, WARN, ERROR
- **Destinations**: Console (Docker logs)
- **Structured**: JSON format for log aggregation

## 🚨 Error Handling

### Restart Strategies

- **Fixed Delay**: 3 restart attempts with 10s delays
- **Failure Recovery**: Automatic job restart on failures
- **State Recovery**: Resume from last successful checkpoint

### Common Issues

1. **Serialization Errors**: Java module access issues
   - **Solution**: Added --add-opens JVM flags
2. **Kafka Connection**: Network connectivity problems
   - **Solution**: Health checks and retry logic
3. **Memory Issues**: Insufficient heap or managed memory
   - **Solution**: Tune TaskManager memory settings

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Testing

```bash
# Start test environment
docker-compose up -d kafka flink-jobmanager flink-taskmanager

# Submit test job
flink run target/stream-processor-1.0.0-SNAPSHOT.jar

# Verify processing
curl http://localhost:8082/jobs
```

### Performance Testing

- **Load Testing**: High-volume order simulation
- **Latency Testing**: End-to-end processing time
- **Throughput Testing**: Maximum sustainable rate

## 📦 Dependencies

- Apache Flink Streaming Java
- Apache Flink Kafka Connector
- Jackson for JSON processing
- Kryo for serialization
- SLF4J + Log4j2 for logging

## 🔮 Future Enhancements

- **Machine Learning**: Anomaly detection models
- **Complex Event Processing**: Multi-stream joins
- **State Management**: Persistent state backends
- **Exactly-Once**: End-to-end consistency guarantees
