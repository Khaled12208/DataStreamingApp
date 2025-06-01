# Deployment Guide

## 📚 Overview

This guide covers various deployment scenarios for the Data Streaming Application, from local development to production-ready deployments.

## 🚀 Quick Start Deployment

### Using the Startup Script (Recommended)

```bash
# Clone repository
git clone <repository-url>
cd DataStreamingApp

# Make startup script executable
chmod +x start-app.sh

# Start the entire application
./start-app.sh
```

This script will:

- ✅ Check Docker prerequisites
- ✅ Build all services
- ✅ Start infrastructure components
- ✅ Perform health checks
- ✅ Display service URLs

## 🐳 Docker Compose Deployment

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- 8GB+ RAM available
- Ports 8080-8083, 5432, 9092 available

### Manual Docker Deployment

```bash
# Build and start all services
docker-compose up -d --build

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Complete cleanup (removes volumes)
docker-compose down -v
```

### Service-by-Service Startup

```bash
# Start infrastructure first
docker-compose up -d zookeeper kafka schema-registry postgres

# Wait for infrastructure to be ready
sleep 30

# Start Flink cluster
docker-compose up -d flink-jobmanager flink-taskmanager

# Start application services
docker-compose up -d order-producer analytics-consumer

# Finally start stream processor
docker-compose up -d stream-processor
```

## 🏗️ Local Development Deployment

### Prerequisites

- Java 17+ (OpenJDK or Oracle)
- Maven 3.6+
- Docker (for infrastructure)
- IDE (IntelliJ IDEA, Eclipse, VS Code)

### Infrastructure Setup

```bash
# Start only infrastructure components
docker-compose up -d kafka postgres flink-jobmanager flink-taskmanager kafka-ui

# Verify infrastructure
curl http://localhost:8080  # Kafka UI
curl http://localhost:8082  # Flink Dashboard
```

### Build Applications

```bash
# Build all modules
mvn clean install

# Or build individually
cd order-producer && mvn clean package
cd ../stream-processor && mvn clean package
cd ../analytics-consumer && mvn clean package
```

### Run Applications Locally

```bash
# Terminal 1: Order Producer
cd order-producer
export SPRING_PROFILES_ACTIVE=local
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
mvn spring-boot:run

# Terminal 2: Analytics Consumer
cd analytics-consumer
export SPRING_PROFILES_ACTIVE=local
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/analytics
mvn spring-boot:run

# Terminal 3: Stream Processor (submit to Flink)
cd stream-processor
flink run -c com.example.streamprocessor.StreamProcessorJob \
  target/stream-processor-1.0.0-SNAPSHOT.jar
```

## 🔧 Configuration Management

### Environment-Specific Configurations

#### Development (application-dev.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
  datasource:
    url: jdbc:postgresql://localhost:5432/analytics
    username: analytics
    password: analytics
logging:
  level:
    com.example: DEBUG
```

#### Production (application-prod.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    properties:
      security.protocol: SASL_SSL
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
logging:
  level:
    root: INFO
```

### Environment Variables

```bash
# Kafka Configuration
export KAFKA_BOOTSTRAP_SERVERS=kafka1:9092,kafka2:9092,kafka3:9092
export SCHEMA_REGISTRY_URL=http://schema-registry:8081

# Database Configuration
export SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/analytics
export SPRING_DATASOURCE_USERNAME=analytics
export SPRING_DATASOURCE_PASSWORD=secure_password

# Flink Configuration
export FLINK_JOBMANAGER_HOST=flink-jobmanager
export FLINK_PARALLELISM=4

# Application Configuration
export SPRING_PROFILES_ACTIVE=prod
export JAVA_OPTS="-Xmx2g -Xms1g"
```

## 🌐 Production Deployment

### Docker Swarm Deployment

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.prod.yml datastreaming

# Check services
docker service ls

# Scale services
docker service scale datastreaming_order-producer=3
docker service scale datastreaming_analytics-consumer=2
```

### Production Docker Compose

```yaml
# docker-compose.prod.yml
version: "3.8"
services:
  order-producer:
    image: order-producer:${VERSION}
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xmx768m -Xms512m
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### Kubernetes Deployment

#### Namespace and ConfigMap

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: data-streaming

---
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: data-streaming
data:
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
  SPRING_PROFILES_ACTIVE: "k8s"
```

#### Order Producer Deployment

```yaml
# k8s/order-producer.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-producer
  namespace: data-streaming
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-producer
  template:
    metadata:
      labels:
        app: order-producer
    spec:
      containers:
        - name: order-producer
          image: order-producer:latest
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              valueFrom:
                configMapKeyRef:
                  name: app-config
                  key: SPRING_PROFILES_ACTIVE
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: order-producer-service
  namespace: data-streaming
spec:
  selector:
    app: order-producer
  ports:
    - port: 8081
      targetPort: 8081
  type: LoadBalancer
```

### Deploy to Kubernetes

```bash
# Apply configurations
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/

# Check deployments
kubectl get pods -n data-streaming
kubectl get services -n data-streaming

# View logs
kubectl logs -f deployment/order-producer -n data-streaming

# Scale deployment
kubectl scale deployment order-producer --replicas=5 -n data-streaming
```

## 🔒 Security Considerations

### Production Security Checklist

- [ ] **SSL/TLS**: Enable HTTPS for all APIs
- [ ] **Kafka Security**: SASL/SSL authentication
- [ ] **Database Security**: Connection pooling with SSL
- [ ] **Secrets Management**: Use Kubernetes secrets or vault
- [ ] **Network Security**: Proper firewall rules
- [ ] **Authentication**: API authentication/authorization
- [ ] **Container Security**: Scan images for vulnerabilities

### Secure Configuration Example

```yaml
# docker-compose.secure.yml
services:
  kafka:
    environment:
      KAFKA_SECURITY_INTER_BROKER_PROTOCOL: SASL_SSL
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
      KAFKA_SSL_KEYSTORE_LOCATION: /etc/kafka/secrets/keystore.jks
      KAFKA_SSL_TRUSTSTORE_LOCATION: /etc/kafka/secrets/truststore.jks
    volumes:
      - ./secrets:/etc/kafka/secrets

  postgres:
    environment:
      POSTGRES_SSL_MODE: require
    volumes:
      - ./ssl-certs:/var/lib/postgresql/ssl
```

## 📊 Monitoring & Observability

### Prometheus + Grafana Stack

```yaml
# monitoring/docker-compose.monitoring.yml
version: "3.8"
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana

volumes:
  grafana-data:
```

### Logging with ELK Stack

```yaml
# logging/docker-compose.logging.yml
version: "3.8"
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.5.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false

  logstash:
    image: docker.elastic.co/logstash/logstash:8.5.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

  kibana:
    image: docker.elastic.co/kibana/kibana:8.5.0
    ports:
      - "5601:5601"
```

## 🚨 Troubleshooting

### Common Deployment Issues

#### 1. Port Conflicts

```bash
# Check port usage
lsof -i :8080
lsof -i :8081

# Kill processes using ports
sudo kill -9 $(lsof -t -i:8080)
```

#### 2. Docker Resource Issues

```bash
# Check Docker resources
docker system df
docker system prune -a

# Increase Docker memory limit
# Docker Desktop: Settings > Resources > Memory > 8GB+
```

#### 3. Kafka Connection Issues

```bash
# Test Kafka connectivity
docker exec -it datastreamingapp-kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

# Check Kafka logs
docker-compose logs kafka
```

#### 4. Database Connection Issues

```bash
# Test PostgreSQL connection
docker exec -it datastreamingapp-postgres-1 psql -U analytics -d analytics -c "SELECT 1;"

# Check database logs
docker-compose logs postgres
```

### Health Check Commands

```bash
# Check all service health
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8082/overview
curl http://localhost:8080

# Comprehensive system check
./health-check.sh
```

## 📈 Performance Tuning

### JVM Tuning

```bash
# Order Producer JVM options
export JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Analytics Consumer JVM options
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+ParallelRefProcEnabled"
```

### Kafka Tuning

```yaml
# Kafka performance configuration
KAFKA_NUM_NETWORK_THREADS: 8
KAFKA_NUM_IO_THREADS: 16
KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
KAFKA_SOCKET_REQUEST_MAX_BYTES: 104857600
```

### Flink Tuning

```yaml
# Flink TaskManager configuration
FLINK_PROPERTIES: |
  taskmanager.numberOfTaskSlots: 4
  taskmanager.memory.process.size: 4096m
  taskmanager.memory.task.heap.size: 1024m
  taskmanager.memory.managed.size: 1024m
```

## 🔄 CI/CD Pipeline

### GitHub Actions Example

```yaml
# .github/workflows/deploy.yml
name: Deploy Data Streaming App
on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: "17"
      - name: Run tests
        run: mvn test

  build-and-deploy:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker images
        run: docker-compose build
      - name: Deploy to staging
        run: docker-compose -f docker-compose.staging.yml up -d
```

---

**Note**: Always test deployments in a staging environment before production. Monitor system metrics and logs during initial deployment phases.
