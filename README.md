# E-commerce Microservices with Saga Pattern

A Spring Boot microservices architecture implementing saga pattern, retry mechanisms, and event-driven coordination for e-commerce order processing.

## Architecture

- **Order Service** (Port 8080): Creates orders and publishes OrderCreatedEvent; orchestrates the saga flow
- **Payment Service** (Port 8081): Listens for payment requests via Kafka; processes payments with retry/fallback
- **ERP Service** (Port 8082): REST API for ERP updates and inventory synchronization
- **Kafka**: Event streaming for asynchronous saga coordination (payment-requests, payment-responses, payment-compensations topics)
- **PostgreSQL**: Separate databases for Order and Payment services
- **Kafka (KRaft mode)**: Self-managed coordination without ZooKeeper

## Patterns Implemented

### 1. Saga Pattern (Event-Driven Orchestration)
- Order Service publishes `OrderCreatedApplicationEvent` (Spring ApplicationEvent)
- OrderSaga listens to application events and coordinates async flows via Kafka
- Distributed transaction across Order → Payment → ERP services
- Compensation transactions (payment rollback) on ERP failures
- Dead Letter Queues (DLQs) for failed events: `saga-start-dlq`, `payment-responses-dlq`, `erp-responses-dlq`

### 2. Event-Driven Architecture
- **OrderCreatedApplicationEvent**: Decouples OrderService from OrderSaga (solves circular dependency)
- **Kafka Topics**: Async communication between services
  - `payment-requests`: Order Service → Payment Service
  - `payment-responses`: Payment Service → Order Service (saga)
  - `payment-compensations`: Compensation flows on ERP failures
- **HTTP Communication**: Synchronous calls
  - OrderSaga → ERP Service (REST API)

### 3. Retry Pattern with Resilience4j
- All saga handlers (`handlePaymentResponse`, `handleERPResponse`) use `@Retry` with 3 max attempts
- 1-second exponential backoff between retries
- `@Recover` fallback methods route failures to DLQs and cancel orders

## Project Structure

```
order-service/
  ├── saga/
  │   └── OrderSaga.java              # Listens to events and coordinates saga flows
  ├── service/
  │   └── OrderService.java           # Creates orders, publishes OrderCreatedApplicationEvent
  ├── entity/
  │   └── Order.java                  # Order entity with status (PENDING, PAYMENT_PROCESSING, COMPLETED, CANCELLED)
  └── event/
      └── OrderCreatedApplicationEvent.java  # Decouples OrderService from OrderSaga

payment-service/
  ├── listener/
  │   └── PaymentEventListener.java    # Listens to payment-requests topic
  ├── service/
  │   └── PaymentService.java          # Processes payments

erp-service/
  ├── controller/
  │   └── ERPController.java           # ERP endpoints for order updates
  └── service/
      └── ERPService.java              # ERP business logic

common/
  └── events/
      ├── OrderEvent.java              # Base event
      ├── OrderCreatedEvent.java        # Kafka domain event (different from ApplicationEvent)
      ├── PaymentProcessedEvent.java
      ├── PaymentFailedEvent.java
      ├── ERPUpdatedEvent.java
      └── ERPFailedEvent.java
```

## Quick Start

### Local Development (Docker Compose) - Recommended
```bash
# Copy environment template and configure secrets (optional)
cp .env.example .env
# Edit .env file with your actual values (optional for local development)

# Build Docker images
./build.sh

# Start infrastructure and services
docker-compose up -d

# Wait for services to start (30-60 seconds)
# Check service health
curl http://localhost:8080/actuator/health

# Vault automatically loads secrets from .env file on startup
# No manual initialization required!

# Services are accessible at:
# Order Service: http://localhost:8080
# Payment Service: http://localhost:8081 (internal only)
# ERP Service: http://localhost:8082 (internal only)
# Kafka: localhost:9092
# Zipkin: http://localhost:9411
# Vault UI: http://localhost:8200 (token: myroot)
```

### Local Kubernetes Development
```bash
# Build Docker images
./build.sh

# Deploy to local Kubernetes (Docker Desktop/minikube)
./deploy-local.sh

# Services accessible via port-forward:
# Order Service: http://localhost:8080
# Vault UI: http://localhost:8200 (token: myroot)
# Zipkin: http://localhost:9411

# Stop port forwarding
pkill -f "kubectl port-forward"

# Clean up
kubectl delete -k k8s/overlays/local
```

### Build and Test
```bash
# Build all services
./gradlew clean build

# Run tests
./gradlew test

# View test reports
# View test reports
open order-service/build/reports/tests/test/index.html
```

## Local Debugging

### Debug Individual Services in IDE

#### IntelliJ IDEA

1. **Open Project**
   - File → Open → Select the project root directory
   - Wait for Gradle to sync

2. **Configure Run Configurations**
   - Edit Configurations (Run → Edit Configurations)
   - Click `+` to add new configuration
   - Select "Application"
   - Fill in:
     - **Name**: Order Service Debug
     - **Main class**: `com.ecommerce.order.OrderServiceApplication`
     - **Module**: `order-service.main`
     - **VM options**: `-Dspring.profiles.active=local`
   - Repeat for Payment Service and ERP Service

3. **Start Services in Docker** (without order-service to debug locally)
   ```bash
   docker-compose up -d postgres-order postgres-payment kafka redis jaeger
   ```

4. **Run Service in Debug Mode**
   - Click the green debug button next to the Run Configuration
   - Set breakpoints in your code (click line number)
   - Request will pause at breakpoints

5. **View Variables and Logs**
   - Right side panel shows: Variables, Watches, Call Stack
   - Bottom panel shows: Console output and application logs

#### VS Code / Spring Boot Extension

1. **Install Extensions**
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack (Pivotal)
   - Debugger for Java (Microsoft)

2. **Create `.vscode/launch.json`**
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Order Service Debug",
         "request": "launch",
         "mainClass": "com.ecommerce.order.OrderServiceApplication",
         "projectName": "order-service",
         "cwd": "${workspaceFolder}",
         "args": "--spring.profiles.active=local"
       }
     ]
   }
   ```

3. **Start Debugging**
   - Press `F5` or select configuration from Run menu
   - Set breakpoints (F9 on line number)

### Debug Kafka Messages

#### View Kafka Topic Messages
```bash
# List all topics
docker exec springboot-ecommerce-kafka-1 kafka-topics \
  --list --bootstrap-server localhost:9092

# Consume messages from a topic (live)
docker exec springboot-ecommerce-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-requests \
  --from-beginning

# Consume from specific topic and partition
docker exec springboot-ecommerce-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-responses \
  --max-messages 5
```

#### Monitor Kafka Consumer Groups
```bash
# List consumer groups
docker exec springboot-ecommerce-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list

# Describe group details
docker exec springboot-ecommerce-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-service \
  --describe
```

### Debug Database Queries

#### Connect to PostgreSQL
```bash
# Order Service Database
psql -h localhost -p 5432 -U order -d orderdb

# Payment Service Database
psql -h localhost -p 5433 -U payment -d paymentdb

# Common queries
\dt                    # List tables
\d orders              # Describe table schema
SELECT * FROM orders;  # View all orders
```

#### Enable SQL Logging
Add to `application.yml`:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Debug Distributed Tracing

#### View Traces in Zipkin
```
http://localhost:9411
```

1. Select service from dropdown: `order-service`, `payment-service`, `erp-service`
2. Find traces by:
   - **Service Name**: Select service and click "Run Query"
   - **Span Name**: Filter by operation (e.g., `POST /orders`)
   - **Duration**: Set min/max duration filters
3. Click trace to see complete saga flow

#### Extract Trace ID from Logs
```bash
# Find trace ID in logs
docker logs springboot-ecommerce-order-service-1 | grep -E "\[order-service,[a-f0-9]{16},[a-f0-9]{16}\]"

# Use trace ID to search in Zipkin UI
```

### Debug with Print Statements and Logs

#### Add Logging in Code
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public void createOrder(CreateOrderRequest request) {
        logger.debug("Creating order for customer: {}", request.getCustomerId());
        logger.info("Order created with ID: {}", order.getId());
        logger.warn("Payment processing delayed for order: {}", order.getId());
        logger.error("Failed to process order: {}", order.getId(), exception);
    }
}
```

#### View Logs by Service
```bash
# Follow order service logs
docker logs -f springboot-ecommerce-order-service-1

# View last 100 lines
docker logs --tail 100 springboot-ecommerce-order-service-1

# Grep for specific patterns
docker logs springboot-ecommerce-order-service-1 | grep "ERROR"
docker logs springboot-ecommerce-order-service-1 | grep "OrderSaga"

# Combine services
docker logs springboot-ecommerce-order-service-1 & docker logs springboot-ecommerce-payment-service-1
```

### Troubleshooting Common Issues

#### Service won't start
```bash
# Check logs for errors
docker logs springboot-ecommerce-order-service-1 | tail -50

# Verify database connectivity
docker exec springboot-ecommerce-postgres-order-1 psql -U order -d orderdb -c "SELECT 1"

# Check port availability
lsof -i :8080  # Check if port 8080 is in use

# If services can't connect to Kafka, restart them after Kafka is ready
docker restart springboot-ecommerce-order-service-1 springboot-ecommerce-payment-service-1 springboot-ecommerce-erp-service-1
```

#### Kafka messages not being consumed
```bash
# Verify Kafka is running
docker exec springboot-ecommerce-kafka-1 kafka-broker-api-versions --bootstrap-server localhost:9092

# Check consumer group lag
docker exec springboot-ecommerce-kafka-1 kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-service \
  --describe

# Check if topic exists
docker exec springboot-ecommerce-kafka-1 kafka-topics \
  --list --bootstrap-server localhost:9092 | grep payment-requests
```

#### JWT token issues
```bash
# Decode JWT token (online tool)
# https://jwt.io

# Manual token validation
curl -X GET http://localhost:8080/auth/validate \
  -H "Authorization: Bearer <your-token>"

# Generate new token
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user"}'
```

### EKS/Production Deployment

**Prerequisites:**
- EKS cluster configured
- kubectl configured for EKS
- Docker images pushed to ECR or container registry
- AWS Load Balancer Controller installed

**Deploy to EKS:**
```bash
# Update registry and version in deploy-eks.sh
REGISTRY="123456789012.dkr.ecr.us-west-2.amazonaws.com"
VERSION="v1.0.0"

# Push images to ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin $REGISTRY
docker tag order-service:latest $REGISTRY/order-service:$VERSION
docker push $REGISTRY/order-service:$VERSION
# Repeat for payment-service and erp-service

# Deploy to EKS
./deploy-eks.sh

# Get external URLs
kubectl get svc -n ecommerce
```

**Features:**
- **LoadBalancer Services**: External access via AWS ALB/NLB
- **Horizontal Scaling**: 3 replicas for order/payment, 2 for ERP
- **Health Checks**: Liveness and readiness probes
- **Resource Limits**: Production-ready CPU and memory limits
- **Vault Integration**: Automatic secret management
- **Observability**: Zipkin tracing and metrics

**Troubleshooting:**
```bash
# Check pod status
kubectl get pods -n ecommerce

# View logs
kubectl logs -f deployment/order-service -n ecommerce

# Check LoadBalancer status
kubectl get svc order-service -n ecommerce

# Clean up
kubectl delete -k k8s/overlays/production
```

## API Usage

### Create Order (Triggers Saga)

**Step 1: Generate JWT Token**
```bash
# Generate token first
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user"}'

# Response: {"accessToken":"eyJ...","tokenType":"Bearer"}
```

**Step 2: Create Order**
```bash
# Use the token from step 1
TOKEN="your-jwt-token-here"

# Docker Compose (recommended)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customerId": "customer-123",
    "amount": 99.99
  }'

# Kubernetes (local with port-forward)
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customerId": "customer-123",
    "amount": 99.99
  }'

# EKS (replace with actual LoadBalancer URL)
curl -X POST http://a1b2c3d4-123456789.us-west-2.elb.amazonaws.com:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "customerId": "customer-123",
    "amount": 99.99
  }'
```

**Response:**
```json
{
  "id": "fcb61f8e-2cdf-4b73-8d2a-35b833fcc61c",
  "customerId": "customer-123",
  "amount": 99.99,
  "status": "PENDING",
  "sagaId": "0b170b9c-94f6-4a63-9e13-5b2c15e18f21",
  "createdAt": "2025-12-10T11:23:32.538736506"
}
```

### List Orders
```bash
TOKEN="your-jwt-token-here"

curl -X GET http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
[
  {
    "id": "fcb61f8e-2cdf-4b73-8d2a-35b833fcc61c",
    "customerId": "customer-123",
    "amount": 99.99,
    "status": "COMPLETED",
    "sagaId": "0b170b9c-94f6-4a63-9e13-5b2c15e18f21",
    "createdAt": "2025-12-10T11:23:32.538736506"
  },
  {
    "id": "edfd92d6-3ddd-437c-ac1e-e633f3811ea0",
    "customerId": "cust-123",
    "amount": 50.00,
    "status": "PENDING",
    "sagaId": "97d70faa-616d-4d97-8429-4b360575cf12",
    "createdAt": "2025-12-10T21:49:12.445008457"
  }
]
```

### Get Order Details
```bash
TOKEN="your-jwt-token-here"

curl -X GET http://localhost:8080/orders/fcb61f8e-2cdf-4b73-8d2a-35b833fcc61c \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "id": "fcb61f8e-2cdf-4b73-8d2a-35b833fcc61c",
  "customerId": "customer-123",
  "amount": 99.99,
  "status": "COMPLETED",
  "sagaId": "0b170b9c-94f6-4a63-9e13-5b2c15e18f21",
  "createdAt": "2025-12-10T11:23:32.538736506"
}
```

### Order Status Flow
1. **PENDING** → Order created, saga initiated
2. **PAYMENT_PROCESSING** → Payment service processed successfully
3. **COMPLETED** → ERP service completed successfully
4. **CANCELLED** → Payment or ERP failure, order cancelled with reason

## Saga Flow Diagram

```
[Order Service]
    ↓ (publishes OrderCreatedApplicationEvent)
[OrderSaga] (event listener)
    ↓ (sends payment-requests to Kafka)
[Payment Service] (Kafka listener)
    ↓ (sends payment-responses to Kafka)
[OrderSaga] (handles payment response)
    ├─ SUCCESS → HTTP call to ERP Service
    └─ FAILURE → cancelOrder()
         ↓
    [ERP Service] (REST API)
        ├─ SUCCESS → OrderSaga completes order
        └─ FAILURE → send payment-compensations + cancelOrder()
```

## Configuration

### Environment Variables

Sensitive configuration is managed via Vault, automatically loaded from `.env` file:

| Variable | Description | Required | Default |
|----------|-------------|----------|----------|
| `ORDER_DB_USERNAME` | Order service database username | No | `order` |
| `ORDER_DB_PASSWORD` | Order service database password | No | `password` |
| `PAYMENT_DB_USERNAME` | Payment service database username | No | `payment` |
| `PAYMENT_DB_PASSWORD` | Payment service database password | No | `password` |
| `REDIS_PASSWORD` | Redis password | No | Empty (no auth) |
| `SLACK_WEBHOOK_URL` | Slack webhook for DLQ notifications | No | Empty (notifications disabled) |
| `JWT_SECRET` | JWT signing secret (≥512 bits for HS512) | Production | `demo-secret-key-for-local-development-only` |

**Setup:**
```bash
# Copy template and configure (optional)
cp .env.example .env
edit .env  # Add your actual values

# Docker Compose automatically loads .env into Vault
docker-compose up -d

# Note: Works without .env file using secure defaults
```

### Kafka Bootstrap Servers
- Docker Compose: `kafka:9092` (internal network)
- External: `localhost:9092`

### Database Connections
- Order Service: `jdbc:postgresql://postgres-order:5432/orderdb`
- Payment Service: `jdbc:postgresql://postgres-payment:5432/paymentdb`

### Vault Configuration (Secret Management)

**Automatic Secret Initialization:**
Vault automatically loads secrets from your `.env` file on startup. No manual initialization required!

**How it works:**
1. **With .env file**: Vault loads all values from `.env` automatically
2. **Without .env file**: Vault uses secure defaults for all secrets
3. **Persistent storage**: Secrets persist between Docker Compose restarts
4. **Database credentials**: Both order-service and payment-service get credentials from Vault

**Verify secrets are loaded:**
```bash
# View order-service secrets
docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=myroot \
  springboot-ecommerce-vault-1 vault kv get secret/order-service

# View payment-service secrets  
docker exec -e VAULT_ADDR=http://127.0.0.1:8200 -e VAULT_TOKEN=myroot \
  springboot-ecommerce-vault-1 vault kv get secret/payment-service

# Access Vault UI: http://localhost:8200 (token: myroot)
```

**Secrets stored in Vault:**
- **order-service**: `spring.datasource.username`, `spring.datasource.password`, `jwt.secret`, `slack.webhook.url`
- **payment-service**: `spring.datasource.username`, `spring.datasource.password`, `spring.data.redis.password`

### Retry Configuration (Resilience4j)
```yaml
resilience4j:
  retry:
    instances:
      saga-operations:
        maxAttempts: 3
        waitDuration: 1000
```

## Resolved Issues

### Circular Dependency (OrderService ↔ OrderSaga)
**Problem**: OrderService needed to call `orderSaga.startSaga()`, but OrderSaga needed OrderService for state mutations.

**Solution**: Implemented event-driven architecture:
- OrderService publishes `OrderCreatedApplicationEvent` (Spring's ApplicationEventPublisher)
- OrderSaga listens via `@EventListener` on OrderCreatedApplicationEvent
- No direct dependency between the two classes

### Kafka Connectivity (Docker Network)
**Problem**: Services couldn't connect to Kafka using `localhost:9092` inside Docker.

**Solution**: Updated `KAFKA_ADVERTISED_LISTENERS` to `kafka:9092` in docker-compose.yml for proper Docker network DNS resolution.

### Port Conflicts
- Payment Service and ERP Service are internal only (no external ports) to avoid conflicts

## Payment Idempotency Implementation

### Problem
In distributed systems with retries, the same payment request can be processed multiple times, leading to duplicate charges. This occurs when:
- A payment is successfully processed but the response is lost
- Kafka retry/redelivery of the same message
- Network timeouts causing automatic retries

### Solution: Idempotency Keys with Redis

**PaymentIdempotencyService** stores processed payments in Redis with a 24-hour TTL:

```
Key: payment:idempotency:{orderId}:{sagaId}
Value: PROCESSED
TTL: 24 hours
```

**Flow:**
1. `PaymentEventListener` receives payment request event
2. Generates idempotency key from order ID + saga ID (unique combination)
3. Checks Redis: has this payment been processed before?
   - YES → Skip payment processing (duplicate request)
   - NO → Process payment → Record in Redis
4. Same logic for compensation events to prevent double refunds

**Example:**
- Order ID: `c44203d2-8d0c-427b-8842-bd3544b1f5b1`
- Saga ID: `26cbb2fb-e622-410c-88d6-7691617fdfb0`
- Idempotency Key: `payment:idempotency:c44203d2-8d0c-427b-8842-bd3544b1f5b1:26cbb2fb-e622-410c-88d6-7691617fdfb0`

**Benefits:**
- ✅ Prevents duplicate charges
- ✅ Handles Kafka redelivery safely
- ✅ Works across service restarts (stored in Redis, not memory)
- ✅ Automatic cleanup after 24 hours



### View Service Logs
```bash
docker logs springboot-ecommerce-order-service-1
docker logs springboot-ecommerce-payment-service-1
docker logs springboot-ecommerce-erp-service-1
```

### Check Kafka Topics
```bash
docker exec springboot-ecommerce-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

### Database Access
```bash
# Order DB
psql -h localhost -p 5432 -U order -d orderdb

# Payment DB
psql -h localhost -p 5433 -U payment -d paymentdb
```

## Testing

Unit tests verify:
- Order creation and status transitions
- Event publishing via ApplicationEventPublisher
- Saga event handling and Kafka message production
- Retry and fallback behavior

Integration tests use testcontainers for:
- Kafka message flow
- Database operations
- Full saga workflow

## Distributed Tracing with Zipkin

### Overview
Zipkin is integrated for distributed tracing across all microservices. Every request is traced from Order Service through Payment Service to ERP Service, allowing you to:
- **Visualize request flow** across services
- **Measure latency** at each service
- **Debug failures** with detailed span information
- **Identify performance bottlenecks** in the saga flow

### Configuration

All services are configured with:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Trace all requests (100%)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Accessing Zipkin UI

Once services are running, open the Zipkin UI:
```
http://localhost:9411
```

### Viewing Traces

1. **Select Service**: Choose from order-service, payment-service, or erp-service in the dropdown
2. **Find Traces**: Search by operation name or trace ID
3. **View Spans**: Click a trace to see all spans (operations) within the request
4. **Check Latency**: See how long each service took to process

### Example Trace Flow

A typical order creation generates a trace like:
```
[order-service] POST /orders
  └─ [order-service] createOrder
    └─ [order-service] publishEvent
      └─ [order-service] kafka.send (payment-requests)
        └─ [payment-service] kafka.listen (payment-requests)
          └─ [payment-service] processPayment
            └─ [payment-service] kafka.send (payment-responses)
              └─ [order-service] kafka.listen (payment-responses)
                └─ [order-service] kafka.send (erp-requests)
                  └─ [erp-service] kafka.listen (erp-requests)
                    └─ [erp-service] updateERP
                      └─ [erp-service] kafka.send (erp-responses)
                        └─ [order-service] kafka.listen (erp-responses)
                          └─ [order-service] updateOrderStatus
```

### Spring Boot Tracing Integration

Spring Boot automatically:
- **Generates trace IDs** for request tracking (unique for each order creation)
- **Propagates trace context** across HTTP requests and Kafka messages
- **Instruments Spring components** (controllers, repositories, messaging)
- **Tags spans** with service name, operation name, and status

### Docker Compose Services

Zipkin runs in docker-compose as `zipkin` service:
- **Image**: `openzipkin/zipkin:latest`
- **UI Port**: 9411 (for viewing traces)
- **Storage**: In-memory (data lost on restart; use persistent backend for production)

### Performance Considerations

**Sampling (Production Optimization):**
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # Sample 10% of requests to reduce overhead
```

Change in individual service `application.yml` if needed.

### Troubleshooting Traces

**No traces appearing in Zipkin UI?**
1. Verify Zipkin is running: `docker ps | grep zipkin`
2. Check service logs for trace IDs: `docker logs springboot-ecommerce-order-service-1 | grep -E "\[order-service,[a-f0-9]{16},[a-f0-9]{16}\]"`
3. Ensure sampling probability is > 0: Check `application.yml`
4. Wait 2-3 seconds after creating order for traces to be flushed

**Trace IDs in logs:**
All service logs include trace IDs in the format: `[trace-id]`. Use this to correlate logs across services.

## API Authentication (JWT)

### Overview
The Order Service is secured with JWT (JSON Web Token) based authentication. All requests to protected endpoints must include a valid JWT token in the Authorization header.

### Token Generation

**Endpoint**: `POST /auth/token`

**Request**:
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user"}'
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzY1NDAzMDY2LCJleHAiOjE3NjU0ODk0NjZ9.Unv07MDSaAVoEpjnSE_IPMnI0PRMBWvoDK2gAW9GVVdCrHwN_lqGfTP7KQiuKSD7BrkGGIfxky7e3HNYXktd-Q",
  "tokenType": "Bearer"
}
```

### Using the Token

Include the token in the `Authorization` header for all protected endpoints:

```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzY1NDAzMDY2LCJleHAiOjE3NjU0ODk0NjZ9.Unv07MDSaAVoEpjnSE_IPMnI0PRMBWvoDK2gAW9GVVdCrHwN_lqGfTP7KQiuKSD7BrkGGIfxky7e3HNYXktd-Q"

# Create order (protected endpoint)
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"customer-123","amount":99.99}'
```

### Token Validation

**Endpoint**: `GET /auth/validate`

Verify a token is valid:
```bash
curl -X GET http://localhost:8080/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

**Valid Response**:
```json
{
  "valid": true,
  "message": "Token is valid",
  "username": "user"
}
```

**Invalid Response**:
```json
{
  "valid": false,
  "message": "Token is invalid",
  "username": null
}
```

### Authentication Configuration

**JWT Settings** (in `order-service/src/main/resources/application.yml`):
```yaml
jwt:
  secret: my-super-secret-jwt-key-for-ecommerce-order-service-that-is-longer-than-512-bits-to-comply-with-hs512-algorithm-requirements-and-security-standards-for-jwt-signing
  expiration: 86400000  # 24 hours in milliseconds
```

**Algorithm**: HS512 (HMAC with SHA-512)
- Requires secret key ≥ 512 bits
- Token expiration: 24 hours

### Demo Credentials

For local testing, the following in-memory users are available:

| Username | Password | Role   |
|----------|----------|--------|
| user     | password | USER   |
| admin    | password | ADMIN  |

**Generate token for demo user**:
```bash
curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"user"}'
```

### Protected Endpoints

The following endpoints require a valid JWT token:

- `POST /orders` - Create order
- `GET /orders/{id}` - Get order details
- `GET /orders` - List orders

### Public Endpoints

These endpoints do NOT require authentication:

- `POST /auth/token` - Generate token
- `GET /auth/validate` - Validate token (optional header)

### Security Architecture

**JwtTokenProvider**: Generates and validates JWT tokens
- Uses `Jwts.builder()` to create signed tokens
- Uses `Jwts.parser().build()` to validate and extract claims

**JwtAuthenticationFilter**: Intercepts requests to extract and validate tokens
- Checks `Authorization` header for `Bearer` token
- Sets Spring Security context with authenticated user
- Allows requests to proceed if token is valid

**SecurityConfig**: Spring Security configuration
- Requires authentication for all `/orders/**` endpoints
- Disables CSRF for API endpoints
- Enables stateless session management (JWT)
- In-memory user store for demo purposes

## Observability

### Metrics & Monitoring
- **Prometheus metrics**: http://localhost:808x/actuator/prometheus
- **Health checks**: http://localhost:808x/actuator/health
- **Circuit breakers**: http://localhost:808x/actuator/circuitbreakers
- **Prometheus UI**: http://localhost:9090
- **Grafana dashboards**: http://localhost:3000 (admin/admin)

### Distributed Tracing
- **Zipkin UI**: http://localhost:9411
- Traces include correlation IDs across all services
- Request flow visualization from Order → Payment → ERP
- Service names: `order-service`, `payment-service`, `erp-service`

### Start Observability Stack
```bash
# Start monitoring and tracing tools
docker-compose -f docker-compose.observability.yml up -d

# Start application services
docker-compose up -d

# Access UIs:
# Zipkin: http://localhost:9411
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

## Code Quality with SonarQube

### Setup
1. **Start SonarQube**: `docker-compose -f docker-compose.observability.yml up -d sonarqube`
2. **Access SonarQube**: http://localhost:9000 (admin/admin)
3. **Generate coverage**: `./gradlew test jacocoTestReport`
4. **Run analysis**: `./gradlew sonarqube -Dsonar.host.url=http://localhost:9000 -Dsonar.login=admin -Dsonar.password=admin`

### Coverage Reports
- **JaCoCo HTML**: `open order-service/build/reports/jacoco/test/html/index.html`
- **SonarQube Dashboard**: http://localhost:9000/dashboard?id=springboot-ecommerce

### Next Steps

- [x] Add payment idempotency keys (Redis) to prevent duplicate charges
- [x] Add distributed tracing (Zipkin) to visualize saga flow
- [x] Add API authentication (OAuth2/JWT)
- [x] Add observability (Prometheus, Grafana, Zipkin)
- [x] Add DLQ monitoring with Slack notifications
- [ ] Implement order status caching (Redis)
- [ ] Implement CQRS for read models
- [ ] Integrate with OAuth2 provider (Google, GitHub)
