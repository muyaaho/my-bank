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

## Development Workflow

**IMPORTANT**: After completing any development work (code changes, bug fixes, new features), **ALWAYS deploy to Kind cluster** to verify the changes work in a Kubernetes environment.

### Standard Development Workflow

1. **Develop Locally** (optional, for quick testing)
2. **Build & Test**
3. **Deploy to Kind** (mandatory)
4. **Verify in Kind**

### Quick Start: Deploy to Kind

```bash
# 🚀 통합 배포 스크립트 (권장) - 모든 것을 한 번에 설치
./deploy-mybank.sh

# 이 스크립트는 자동으로 다음을 수행합니다:
# 1. Kind 클러스터 생성 (포트 매핑: 80, 443, 30000-30002)
# 2. Gradle 빌드 및 Docker 이미지 빌드
# 3. Kind로 이미지 로드
# 4. /etc/hosts 도메인 자동 설정 (*.mybank.com)
# 5. 자체 서명 TLS 인증서 생성 (CA + 와일드카드 인증서)
# 6. Istio Service Mesh 설치 (버전 1.27.3)
# 7. Kubernetes Namespace 생성 및 TLS 시크릿 적용
# 8. 인프라 서비스 배포 (PostgreSQL, MongoDB, Redis, Kafka)
# 9. 마이크로서비스 순차 배포 (Service Discovery → Gateway → 비즈니스 서비스)
# 10. Istio Gateway 및 VirtualService 설정

# 배포 후 접속:
# - Frontend: https://app.mybank.com (또는 http://localhost:30000)
# - API: https://api.mybank.com
# - Eureka: https://eureka.mybank.com

# 단계별 배포 (기존 방식)
./scripts/generate-certs.sh          # 1. 인증서 생성
./scripts/setup-hosts.sh             # 2. 도메인 설정
./scripts/deploy-complete-system.sh  # 3. 시스템 배포
```

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
# Docker Compose (all services - for local development only)
docker-compose up -d
docker-compose logs -f [service-name]
docker-compose down

# Kubernetes (Kind) - Full deployment (RECOMMENDED)
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

### Kind Deployment: Individual Service Updates

After making changes to a specific service, rebuild and redeploy just that service:

```bash
# 1. Build the specific service
./gradlew :auth-service:build -x test

# 2. Build Docker image
docker build -t mybank/auth-service:latest -f auth-service/Dockerfile .

# 3. Load to Kind cluster
kind load docker-image mybank/auth-service:latest --name mybank-cluster

# 4. Restart deployment to use new image
kubectl rollout restart deployment/auth-service -n mybank

# 5. Watch rollout status
kubectl rollout status deployment/auth-service -n mybank

# 6. Verify new pods are running
kubectl get pods -n mybank -l app=auth-service

# 7. Check logs
kubectl logs -f deployment/auth-service -n mybank
```

### Kind Deployment: Frontend Updates

```bash
# 1. Build frontend
cd app
npm run build

# 2. Build Docker image
docker build -t mybank/frontend:latest .

# 3. Load to Kind
kind load docker-image mybank/frontend:latest --name mybank-cluster

# 4. Restart deployment
kubectl rollout restart deployment/frontend -n mybank

# 5. Verify
kubectl get pods -n mybank -l app=frontend
kubectl logs -f deployment/frontend -n mybank

# Access: https://app.mybank.com
```

### Kind Deployment: Quick Rebuild All Services

```bash
# Use this after multiple service changes
./gradlew clean build -x test && \
docker-compose build && \
kind load docker-image mybank/api-gateway:latest --name mybank-cluster && \
kind load docker-image mybank/auth-service:latest --name mybank-cluster && \
kind load docker-image mybank/pfm-core-service:latest --name mybank-cluster && \
kind load docker-image mybank/payment-service:latest --name mybank-cluster && \
kind load docker-image mybank/investment-service:latest --name mybank-cluster && \
kind load docker-image mybank/frontend:latest --name mybank-cluster && \
kubectl rollout restart deployment -n mybank
```

### Verifying Kind Deployment

```bash
# Check all pods are running
kubectl get pods -n mybank

# Check services
kubectl get svc -n mybank

# Check Istio Gateway
kubectl get gateway -n mybank
kubectl get virtualservice -n mybank

# Test API connectivity
curl -k https://api.mybank.com/actuator/health

# Test frontend
curl -k https://app.mybank.com

# View all logs
kubectl logs -f -l tier=backend -n mybank --all-containers --max-log-requests=10
```

## Key Implementation Patterns

### 1. API Gateway Routing

Routes are defined in `api-gateway/src/main/resources/application.yml`:
- All routes follow pattern: `/api/v1/{service}/**`
- JWT validation via `JwtAuthenticationFilter` (except auth endpoints)
- Service discovery via Eureka (`lb://service-name`)

### 2. JWT Authentication Flow (Production Standard)

**Architecture Pattern**: Stateless JWT + Token Blacklist
**Industry Standard**: Netflix, Uber, Spotify

**Why This Pattern?**
- ❌ **Avoid**: JWT + Redis Session (anti-pattern in MSA)
  - Session storage causes serialization issues
  - 2 Redis operations per request (READ + WRITE)
  - Defeats JWT's stateless nature
- ✅ **Use**: JWT + Blacklist (production pattern)
  - JWT carries all user data (stateless)
  - Redis only stores revoked tokens (minimal memory)
  - 1 Redis operation per request (READ blacklist)
  - 10x faster performance

**Flow**:
1. **Login** (`/api/v1/auth/login`)
   - User provides credentials
   - Auth Service validates and generates JWT
   - JWT payload contains: `userId`, `email`, `name`, `roles`, `exp`
   - **No session created** - JWT is the source of truth

2. **API Request** (with JWT)
   ```
   Authorization: Bearer <jwt_token>
   ```
   - **Step 1**: `JwtAuthenticationWebFilter` (Order: 1)
     - Validates JWT signature using secret key
     - Checks expiration time
     - Extracts user info from JWT claims
     - Adds headers for downstream services:
       - `X-User-Id`: User's unique identifier
       - `X-User-Email`: User's email
       - `X-User-Name`: User's display name
       - `X-User-Roles`: Comma-separated roles
       - `X-Token`: Original JWT token
     - Location: `api-gateway/src/main/java/com/mybank/gateway/filter/JwtAuthenticationWebFilter.java`

   - **Step 2**: `TokenBlacklistFilter` (Order: 2)
     - Checks if token hash exists in Redis blacklist
     - Hash: SHA-256 of token (prevents token exposure)
     - Redis key: `mybank:blacklist:<token_hash>`
     - If blacklisted → 401 Unauthorized
     - If valid → proceed to backend service
     - Location: `api-gateway/src/main/java/com/mybank/gateway/filter/TokenBlacklistFilter.java`

3. **Logout** (`/api/v1/auth/logout`)
   - Frontend sends JWT in Authorization header
   - Auth Service adds token hash to Redis blacklist
   - TTL: 24 hours (matches JWT expiration)
   - Service: `SessionBlacklistService` in `common-lib`
   - Location: `common/src/main/java/com/mybank/common/session/SessionBlacklistService.java`

4. **Backend Services**
   - Receive request with `X-User-*` headers
   - No JWT parsing needed (already validated by gateway)
   - Use `X-User-Id` for authorization and business logic

**Security**:
- JWT Secret: "mybank360-super-secret-key-for-jwt-token-generation-minimum-256-bits"
- Token hashing: SHA-256 (prevents token exposure in logs/monitoring)
- Blacklist cleanup: Automatic via Redis TTL

**Performance**:
- Before (Session): 2 Redis ops + deserialization + 140+ bytes per request
- After (Blacklist): 1 Redis op + 8 bytes per request (hash only)
- Result: **10x faster**, **95% less memory**

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

### 6. Frontend API Integration & Authentication

**Authentication State Management**:
- **Zustand Store** (`app/stores/authStore.ts`): Single source of truth for auth state
- **Persist Middleware**: Stores user, accessToken, refreshToken in localStorage
- **AuthProvider** (`app/app/providers.tsx`): Restores tokens to apiClient on page load
- **Page Refresh Resilience**: Login state persists across page refreshes

```typescript
// authStore.ts
interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  clearAuth: () => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
}

// Login flow
const { setAuth } = useAuthStore();
const response = await authApi.login(data);
setAuth(user, accessToken, refreshToken);  // Persists to localStorage
apiClient.setAuth(accessToken, refreshToken);  // Sets in memory

// Page load flow (app/providers.tsx)
useEffect(() => {
  if (accessToken && refreshToken) {
    apiClient.setAuth(accessToken, refreshToken);  // Restore from localStorage
  }
}, [accessToken, refreshToken]);
```

**API Client** (`app/lib/api/client.ts`):
- Automatic JWT token injection in request headers
- Token refresh on 401 response (updates both apiClient and authStore)
- Redirect to login on auth failure
- Tokens stored in memory (not localStorage) - authStore is the source

**Token Refresh Flow**:
1. API returns 401
2. apiClient intercepts and calls refresh endpoint
3. On success: Updates tokens in both apiClient (memory) and authStore (localStorage)
4. Retries original request with new token
5. On failure: Clears auth and redirects to /login

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
