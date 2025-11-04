# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MyBank is a cloud-native fintech platform implementing **Microservices Architecture (MSA)** and **Event-Driven Architecture (EDA)** patterns. It provides personal financial management, investment services, payment/transfer capabilities with Spring Boot 3, Spring Cloud, Kafka, and Next.js 14.

## Architecture

### Microservices Structure

- **Infrastructure**:
  - **Istio Service Mesh** (1.27.3): Service discovery, traffic management, security
  - **Kubernetes**: Container orchestration (Kind for local development)
  - `api-gateway` (port 8080): JWT authentication, request routing (Spring Cloud Gateway)

- **Business Services**:
  - `auth-service` (port 8081): OAuth 2.0, JWT, authentication (PostgreSQL)
  - `user-service` (port 8085): User profile management (PostgreSQL)
  - `pfm-core-service` (port 8082): Asset aggregation, spending analysis (MongoDB, Redis)
  - `payment-service` (port 8083): Transfers, payment history (MongoDB)
  - `investment-service` (port 8084): Round-up investing, portfolio (MongoDB)

- **Shared Library**:
  - `common-lib`: DTOs, events, Kafka configs, utilities

### Event-Driven Communication

Services communicate asynchronously via **Kafka topics**:

- **payment-completed**: Published by `payment-service` → Consumed by `investment-service` for round-up logic
- Event classes are in `common-lib/src/main/java/com/mybank/common/event/`
- Consumers use `@KafkaListener` with manual acknowledgment for reliability

Example: When a payment of 3,450 KRW completes, `PaymentCompletedEvent` triggers automatic investment of 550 KRW (round-up to 4,000 KRW) in the investment service.

## Common Development Commands

### Build and Test

```bash
# Build all services (skip tests for speed)
./gradlew clean build -x test

# Build specific service
./gradlew :auth-service:build

# Run tests for all services
./gradlew test

# Run tests for specific service
./gradlew :pfm-core-service:test

# Run single test class
./gradlew :auth-service:test --tests AuthServiceTest

# Run single test method
./gradlew :auth-service:test --tests AuthServiceTest.testUserRegistration
```

### Running Services Locally

```bash
# 1. Start infrastructure (PostgreSQL, MongoDB, Redis, Kafka)
docker-compose up -d

# 2. Start services IN ORDER (each in separate terminal):

# Service Discovery (wait ~15s for startup)
./gradlew :service-discovery:bootRun

# Config Server (wait for Eureka registration)
./gradlew :config-server:bootRun

# API Gateway (wait for Eureka registration)
./gradlew :api-gateway:bootRun

# Business services (can start in parallel)
./gradlew :auth-service:bootRun
./gradlew :pfm-core-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :investment-service:bootRun

# 3. Verify services at http://localhost:8761 (Eureka dashboard)
```

### Frontend Development

```bash
cd app

# Install dependencies
npm install

# Development server
npm run dev  # http://localhost:3000

# Build for production
npm run build
npm start

# Run tests
npm test
npm run test:coverage

# Type checking
npm run type-check

# Linting
npm run lint
```

### Docker and Kubernetes

```bash
# Docker Compose (all services)
docker-compose up -d
docker-compose logs -f [service-name]
docker-compose down

# Kubernetes (Kind) - Full deployment
./scripts/deploy-complete-system.sh

# Access services
# Frontend: https://app.mybank.com (or http://localhost:30000)
# API Gateway: https://api.mybank.com
# Eureka: https://eureka.mybank.com

# Kubernetes operations
kubectl get pods -n mybank
kubectl logs -f deployment/[service-name] -n mybank
kubectl describe pod [pod-name] -n mybank

# Cleanup
kind delete cluster --name mybank-cluster
```

## Key Implementation Patterns

### 1. API Gateway Routing

Routes are defined in `api-gateway/src/main/resources/application.yml`:
- All routes follow pattern: `/api/v1/{service}/**`
- JWT validation via `JwtAuthenticationFilter` (except auth endpoints)
- Service discovery via Eureka (`lb://service-name`)

### 2. JWT Authentication Flow

1. User calls `/api/v1/auth/login` → receives JWT token
2. Frontend stores token, includes in `Authorization: Bearer <token>` header
3. API Gateway validates JWT via `JwtAuthenticationWebFilter` before forwarding to services
4. Gateway extracts user info from JWT and adds headers to downstream requests:
   - `X-User-Id`: User's unique identifier
   - `X-User-Email`: User's email
   - `X-User-Name`: User's display name
   - `X-User-Roles`: Comma-separated roles
5. Backend services read user context from these headers (no JWT parsing needed)
6. Token secret: "mybank360-super-secret-key-for-jwt-token-generation-minimum-256-bits" (must match in gateway + auth-service)

### 3. Kafka Event Publishing

**All events extend BaseEvent** in `common-lib`:
```java
public abstract class BaseEvent {
    protected String eventId;      // UUID for idempotency
    protected String eventType;    // Event discriminator
    protected LocalDateTime timestamp;
    protected String correlationId; // For tracing
    protected String userId;       // For filtering
}
```

**Producer** (e.g., in `payment-service`):
```java
PaymentCompletedEvent event = PaymentCompletedEvent.builder()
    .eventId(UUID.randomUUID().toString())
    .eventType("PAYMENT_COMPLETED")
    .timestamp(LocalDateTime.now())
    .userId(payment.getUserId())
    .paymentId(payment.getId())
    .accountId(payment.getFromAccountId())
    .amount(payment.getAmount())
    .build();

// Use paymentId as key for partition ordering
kafkaTemplate.send("payment-completed", event.getPaymentId(), event);
```

**Consumer** (e.g., in `investment-service`):
```java
@KafkaListener(topics = "payment-completed", groupId = "investment-service")
public void consume(@Payload PaymentCompletedEvent event, Acknowledgment ack) {
    try {
        // Check idempotency using eventId
        if (alreadyProcessed(event.getEventId())) {
            ack.acknowledge();
            return;
        }

        // Process event
        roundUpService.processRoundUp(event);

        // Mark as processed and acknowledge
        markProcessed(event.getEventId());
        ack.acknowledge();
    } catch (Exception e) {
        // Don't acknowledge - message will be redelivered
        log.error("Failed to process event: {}", event.getEventId(), e);
    }
}
```

**Kafka Configuration**:
- KRaft mode (no Zookeeper dependency)
- Idempotent producers: `enable.idempotence=true`
- Acknowledgment: `acks=all` for durability
- Manual offset commits for at-least-once delivery
- JSON serialization with trusted packages: `com.mybank.*`

### 4. Redis Caching Pattern

`pfm-core-service` uses Cache-Aside pattern:
```java
@Cacheable(value = "assets", key = "#userId")
public AssetSummary getAssets(String userId)

@CacheEvict(value = "assets", key = "#userId")
public void syncAssets(String userId)
```

**Cache Configuration**:
- Default TTL: 30 minutes
- JSON serialization with Jackson (JavaTimeModule for dates)
- Graceful degradation: CacheErrorHandler logs errors but doesn't throw exceptions
- If Redis is down, services continue without caching

### 5. Distributed Locking (Redis)

`payment-service` uses Redis distributed locks to prevent duplicate payments:
```java
// Acquire lock before processing payment
String lockKey = "payment:lock:" + userId + ":" + accountId;
Boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));

if (Boolean.TRUE.equals(acquired)) {
    try {
        // Process payment
    } finally {
        redisTemplate.delete(lockKey);
    }
}
```

- Lock TTL: 30 seconds
- Atomic lock acquisition with `setIfAbsent`
- Prevents race conditions in concurrent payment requests

### 6. Frontend API Integration

Frontend uses Axios client with interceptors (`app/lib/api/client.ts`):
- Automatic JWT token injection
- Token refresh on 401
- Redirect to login on auth failure

React Query hooks in `app/lib/hooks/` for data fetching:
```typescript
const { data, isLoading } = useQuery({
  queryKey: ['assets'],
  queryFn: async () => {
    const response = await pfmApi.getAssets();
    return response.data;
  }
});
```

## Configuration Management

### Application Configuration

Each service has `src/main/resources/application.yml`:
- **Server port**: Must be unique per service
- **Eureka client**: Points to service-discovery:8761
- **Data sources**: PostgreSQL (auth), MongoDB (others), Redis (caching)
- **Kafka**: Bootstrap servers at localhost:9092
- **Management endpoints**: Actuator + Prometheus metrics

### Environment-Specific Config

For local development:
- Use `localhost` for all infrastructure
- Default credentials in `docker-compose.yml`

For Kubernetes:
- ConfigMaps in `k8s/config/`
- Service names resolve via DNS (e.g., `postgres.mybank.svc.cluster.local`)

## Testing Strategy

### Backend Tests

- Unit tests: Mock dependencies, test business logic
- Integration tests: Use `@SpringBootTest` with testcontainers
- Kafka tests: Use `@EmbeddedKafka` from `spring-kafka-test`

Example test location:
```
auth-service/src/test/java/com/mybank/auth/
├── service/AuthServiceTest.java
├── controller/AuthControllerTest.java
└── repository/UserRepositoryTest.java
```

### Frontend Tests

Located in `app/__tests__/`:
- Component tests: Testing Library + Jest
- Store tests: Zustand state management
- Hook tests: React Query hooks
- E2E tests: Playwright (configured)

## Database Schemas

### PostgreSQL (auth-service)
- **users**: id, email, password (BCrypt), name, phone_number, roles, is_active, is_locked, failed_login_attempts, fido2_credential_id, created_at, updated_at, last_login_at
- **user_roles**: Collection table for roles
- **oauth_providers**: Kakao OAuth integration

### PostgreSQL (user-service)
- **users**: User profile data (separate from auth credentials)

### MongoDB Collections
- **pfm-core-service**:
  - `assets`: User assets from banks, cards, securities
  - `transactions`: Transaction history with categories

- **payment-service**:
  - `payments`: Payment/transfer records with status tracking

- **investment-service**:
  - `investments`: Investment positions and round-up history

## Common Issues and Solutions

### Service Won't Start
- Check if required services are running (Eureka, databases)
- Verify port availability: `lsof -i :<port>`
- Check application.yml for correct connection strings

### Kafka Connection Failed
- Ensure Kafka is running: `docker-compose ps kafka`
- Check bootstrap servers config matches docker-compose (localhost:9092 for local, kafka:9093 for docker network)
- View Kafka UI: http://localhost:8090

### Frontend Can't Connect to Backend
- Verify API Gateway is running on port 8080
- Check NEXT_PUBLIC_API_URL environment variable
- Ensure JWT token is valid (check browser DevTools → Application → Local Storage)

### Database Connection Issues
- PostgreSQL (auth): Verify at localhost:5432, credentials: mybank/mybank123
- PostgreSQL (user): Verify at localhost:5433, credentials: mybank_user/mybank_user123
- MongoDB: Verify at localhost:27017, credentials: root/rootpassword
- Redis: Verify at localhost:6379 (no password for local development)

## Service Dependencies

**Startup Order Matters:**
1. Infrastructure: docker-compose (PostgreSQL, MongoDB, Redis, Kafka)
2. service-discovery (Eureka)
3. config-server
4. api-gateway
5. Business services (any order)
6. frontend

**Service Communication:**
- API calls: via API Gateway → Service Discovery → Target service
- Events: via Kafka topics (asynchronous, fire-and-forget)
- Caching: Redis for session, asset data, rankings

## Istio Service Mesh Configuration

When deployed to Kubernetes, MyBank uses Istio for:
- **Service Discovery**: Automatic service registration and discovery
- **Traffic Management**: Load balancing, retries, timeouts
- **Security**: Mutual TLS (mTLS) between services
- **Observability**: Distributed tracing with Jaeger

### Gateway and Virtual Services

**Istio Gateway** (port 443 with TLS):
- Handles external traffic at `*.mybank.com`
- TLS termination with cert stored in `mybank-tls-cert` secret
- Routes to frontend (`app.mybank.com`) and API (`api.mybank.com`)

**Virtual Services**:
- Frontend: Routes `https://app.mybank.com` → frontend service:3000
- API: Routes `https://api.mybank.com/api/v1/*` → api-gateway:8080
- CORS policy enabled on API routes

**Access Services**:
- Frontend: https://app.mybank.com or http://localhost:30000 (NodePort)
- API Gateway: https://api.mybank.com
- Eureka: https://eureka.mybank.com

## Deployment Scripts

- `./scripts/deploy-complete-system.sh`: Full automated deployment to Kind (includes Istio setup)
- `./scripts/generate-certs.sh`: TLS certificates for Istio ingress
- `./scripts/setup-hosts.sh`: Configure /etc/hosts for `*.mybank.com` domains
- `./scripts/install-argocd.sh`: Install ArgoCD for GitOps

## Monitoring and Observability

- **Prometheus**: http://localhost:9090 - Metrics collection
- **Grafana**: http://localhost:3001 (admin/admin) - Dashboards
- **Kafka UI**: http://localhost:8090 - Topic monitoring
- **Eureka**: http://localhost:8761 - Service registry
- **Actuator endpoints**: Each service exposes `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

## Technology Versions

- Java: 17+
- Spring Boot: 3.2.0
- Spring Cloud: 2023.0.0
- Node.js: 20+ (for frontend)
- Kafka: 3.6.0
- PostgreSQL: 16
- MongoDB: 7
- Redis: 7

## Code Style Guidelines

### Backend
- Use Lombok for boilerplate reduction (@Data, @Builder, @Slf4j)
- Follow Spring conventions: @Service, @Repository, @RestController
- Event classes extend BaseEvent (common-lib)
- Use BigDecimal for money amounts (never float/double for currency)
- **Aggregate Entity IDs**: Use UUID (String) for all MongoDB document IDs (not ObjectId)
  - Example: `private String id;` with `@Id` annotation
  - Generates UUID on creation for distributed system compatibility
  - Enables event sourcing and cross-service references

### Frontend
- TypeScript strict mode
- Functional components with hooks
- React Query for server state
- Zustand for client state (auth)
- Tailwind CSS for styling
