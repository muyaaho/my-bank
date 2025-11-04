# Architecture Refactoring Summary

## Overview

This document summarizes the major architectural refactoring implemented to separate authentication and user management services with enhanced security features.

## Key Changes

### 1. Service Separation: auth-service → auth-service + user-service

**Before:**
- Single `auth-service` handled both authentication and user management
- User entity stored in `mybank_auth` database

**After:**
- **auth-service (Port 8081)**: Handles authentication only
  - Login/Logout
  - JWT token generation
  - OAuth 2.0
  - Password management
  - Database: `mybank_auth` (PostgreSQL on port 5432)
  - Entity: `AuthCredential` (email, password, login attempts, lock status)

- **user-service (Port 8085)**: Manages user profiles
  - User CRUD operations
  - Role and permission management
  - User search
  - Database: `mybank_user` (PostgreSQL on port 5433)
  - Entity: `User` (profile, roles, permissions)

### 2. Enhanced Global Session Management

**Implementation:**
- `EnhancedSessionService` in `common-lib`
- Supports both blocking and reactive operations
- Redis-based session storage

**Features:**
- **Sliding window expiration**: 30-minute TTL, refreshed on each request
- **Token blacklist**: Revoked tokens stored for 24 hours
- **Token validation**: Quick lookup via `mybank:token:{jwt}` → userId
- **Session data**: Stored at `mybank:session:{userId}`

**Redis Key Patterns:**
```
mybank:session:{userId}       # UserSession object (30 min TTL)
mybank:token:{jwt}            # userId mapping (24 hours TTL)
mybank:blacklist:{jwt}        # Revoked tokens (24 hours TTL)
```

### 3. API Gateway Enhanced Security

**Components:**
1. **JwtAuthenticationWebFilter**
   - Validates JWT signature and expiration
   - Extracts user claims (userId, email, roles)
   - Adds headers: `X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-Token`
   - Sets `ReactiveSecurityContextHolder`

2. **RedisSessionWebFilter**
   - Checks if token is blacklisted
   - Validates session exists in Redis
   - Implements sliding window (refreshes TTL)
   - Rejects expired or invalid sessions

**Flow:**
```
Client Request
    ↓
[JWT Validation]
    ↓
[Redis Session Check]
    ↓
[Refresh Session TTL]
    ↓
[Add User Headers]
    ↓
Downstream Service
```

### 4. Microservice Header-Based Authentication

**Implementation:**
- `HeaderAuthenticationFilter` in `common-lib`
- `MicroserviceSecurityConfig` base class

**Features:**
- Extracts user from headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`)
- Creates `UserPrincipal` and sets `SecurityContext`
- Enables method-level security with `@PreAuthorize`

**Usage in Services:**
```java
@GetMapping("/assets")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<AssetSummary> getAssets() {
    UserPrincipal user = (UserPrincipal) SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();

    String userId = user.getUserId();
    // ...
}
```

### 5. Inter-Service Communication (Feign)

**FeignClient:**
- `UserServiceClient` in `common-lib`
- Used by auth-service to create/retrieve user profiles

**Configuration:**
- `FeignConfig` with request interceptor
- Propagates user context headers
- Endpoints: `/internal/users/*`

**Example:**
```java
// In auth-service
UserResponseDto user = userServiceClient.createUser(
    CreateUserRequestDto.builder()
        .id(userId)
        .email(email)
        .name(name)
        .roles(Set.of("USER"))
        .build()
);
```

### 6. Database Separation

**Before:**
- Single `mybank_auth` database (PostgreSQL)

**After:**
- **mybank_auth** (Port 5432): Auth credentials only
  - auth_credentials
  - oauth_providers
  - login_attempts

- **mybank_user** (Port 5433): User profiles
  - users
  - user_roles
  - user_permissions

### 7. Security Features

#### API Gateway
- Spring Security WebFlux
- JWT + Redis session validation
- Automatic token blacklisting on logout
- Sliding window session expiration

#### Microservices
- Spring Security with header-based auth
- Stateless (no session creation)
- Method-level authorization
- Internal endpoints accessible without auth

#### Authentication Flow
1. **Register/Login**: JWT issued + Redis session created
2. **Request**: Gateway validates JWT + checks Redis session
3. **Refresh Session**: TTL extended on each valid request
4. **Logout**: Session deleted + token blacklisted

### 8. API Routes

**Public (No Auth):**
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

**Protected (JWT + Session Required):**
- `/api/v1/user/**` → user-service
- `/api/v1/pfm/**` → pfm-core-service
- `/api/v1/payment/**` → payment-service
- `/api/v1/invest/**` → investment-service

### 9. Deployment Updates

**Docker Compose:**
- Added `postgres-user` on port 5433
- Separated auth and user databases

**Kubernetes:**
- Added `user-service.yaml` deployment
- ConfigMaps updated for new service
- Service mesh communication via Istio

**Build Script:**
- Updated `build-images.sh` to include user-service

## Benefits

1. **Separation of Concerns**
   - Authentication logic isolated from user management
   - Each service has single responsibility

2. **Enhanced Security**
   - Two-layer validation (JWT + Redis session)
   - Token blacklist prevents reuse after logout
   - Sliding window prevents session fixation

3. **Scalability**
   - Services can scale independently
   - Redis provides fast session lookup
   - Microservices communicate efficiently via Feign

4. **Maintainability**
   - Clear boundaries between services
   - Easier to test and debug
   - Common security logic in shared library

## Migration Path

For existing deployments:

1. **Database Migration**
   ```sql
   -- Create new user database
   CREATE DATABASE mybank_user;

   -- Migrate user data from auth to user DB
   -- (Manual data migration script required)
   ```

2. **Deploy Services**
   ```bash
   # Start infrastructure
   docker-compose up -d

   # Build images
   ./build-images.sh

   # Deploy to Kubernetes
   kubectl apply -f k8s/services/user-service.yaml
   ```

3. **Update Clients**
   - Frontend: No changes required (JWT flow unchanged)
   - Internal services: Update to use UserServiceClient

## Testing

**Integration Tests:**
- `AuthenticationFlowIntegrationTest`: Tests auth/user separation
- Validates Feign client communication
- Tests JWT generation and session creation

**Manual Testing:**
```bash
# 1. Start services
docker-compose up -d
./start-services.sh

# 2. Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","name":"Test User"}'

# 3. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# 4. Access protected endpoint
curl http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer {access_token}"
```

## Future Enhancements

1. **Service-to-Service JWT**: Internal auth tokens for Feign calls
2. **Rate Limiting**: Per-user rate limits via Redis
3. **Session Analytics**: Track active sessions, concurrent logins
4. **Multi-Factor Authentication**: TOTP, SMS, Biometric
5. **OAuth Providers**: Google, Apple, etc.

## References

- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Redis Session Management](https://redis.io/docs/manual/keyspace/)
