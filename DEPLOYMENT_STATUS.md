# MyBank Deployment Status - 2025-11-03

## ✅ Successfully Completed

### 1. Auth/User Service Separation
- **auth-service**: Authentication only (JWT, OAuth 2.0)
  - Database: `postgres-auth:5432/mybank_auth`
  - Stores: AuthCredential (email, password, login attempts)
  - Endpoints: `/api/auth/**`
  - Status: **2/2 Running** ✅

- **user-service**: User profile management
  - Database: `postgres-user:5432/mybank_user`
  - Stores: User (profile, roles, permissions)
  - Endpoints: `/api/v1/user/**` (internal `/internal/users/**`)
  - Status: **2/2 Running** ✅

### 2. MongoDB Autoconfiguration Fixed
- Excluded MongoDB from auth-service and user-service (PostgreSQL only)
- Only business services (pfm, payment, investment) use MongoDB

### 3. Eureka Discovery Disabled
- All services configured to disable Eureka (using Istio service mesh instead)
- `EUREKA_CLIENT_ENABLED: false` in common-config

### 4. Database Connections Fixed
- **auth-service**: Connects to `postgres-auth:5432` ✅
- **user-service**: Connects to `postgres-user:5432` ✅
- Both services using separate PostgreSQL instances

### 5. Security Configuration Fixed
- **auth-service**: Uses custom SecurityConfig (excludes MicroserviceSecurityConfig)
- **user-service**: Uses MicroserviceSecurityConfig via @ComponentScan
- No bean conflicts

### 6. Docker Images Built & Loaded
- `mybank/auth-service:latest` - Eclipse Temurin 21 JRE Alpine
- `mybank/user-service:latest` - Eclipse Temurin 17 JRE (multi-stage build)
- Both images loaded into Kind cluster

## 📊 Current Pod Status

```
auth-service-64565c4fcf-x2hgc         2/2     Running
auth-service-64565c4fcf-zmlfx         2/2     Running
user-service-6c474b84b5-mmrph         2/2     Running
user-service-6c474b84b5-xqsvq         2/2     Running
api-gateway-fc9cd76d4-*               2/2     Running
frontend-*                            2/2     Running
postgres-auth-*                       2/2     Running
postgres-user-*                       2/2     Running
mongodb-*                             2/2     Running
redis-*                               2/2     Running
```

## ⚠️ Known Issues

### 1. Business Services Still Crashing
The following services are in CrashLoopBackOff:
- `payment-service`
- `investment-service`
- `pfm-core-service`
- `kafka` (one instance)

**Reason**: These services likely have similar issues with:
- MongoDB connection configuration
- Kafka connection configuration
- Service discovery (still trying to connect to Eureka)

**Fix Required**: Apply the same fixes as auth/user services to each business service.

### 2. Access Testing Not Complete
Cannot test API endpoints without:
1. `/etc/hosts` entry (requires sudo)
2. Trusting CA certificate (requires sudo)

The infrastructure is ready but user needs to complete local configuration.

## 🔧 Required User Actions

### Step 1: Trust CA Certificate (macOS)
```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /Users/kimhyeonwoo/Documents/GitHub/mybank/certs/ca.crt
```

### Step 2: Add Hosts Entries
```bash
echo "127.0.0.1 mybank.local api.mybank.local app.mybank.local" | sudo tee -a /etc/hosts
```

### Step 3: Verify Port Forwarding is Running
```bash
ps aux | grep "kubectl port-forward"
```

Should show:
```
kubectl port-forward -n istio-system svc/istio-ingressgateway 8443:443 8080:80
```

If not running:
```bash
cd /Users/kimhyeonwoo/Documents/GitHub/mybank
./port-forward.sh
```

### Step 4: Test API Endpoints

#### Test auth-service health (via Istio):
```bash
curl -k https://api.mybank.local:8443/api/v1/auth/health
```

Expected response:
```json
{"success":true,"data":"Auth Service is healthy","timestamp":"..."}
```

#### Test user-service health:
```bash
curl -k https://api.mybank.local:8443/api/v1/user/health
```

#### Test registration:
```bash
curl -X POST https://api.mybank.local:8443/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.local",
    "password": "password123",
    "name": "Test User",
    "phoneNumber": "010-1234-5678"
  }' \
  --cacert certs/ca.crt
```

## 📝 Architecture Summary

### Request Flow:
```
Browser (HTTPS:8443)
    ↓
Port Forward (localhost:8443 → cluster 443)
    ↓
Istio Ingress Gateway (TLS termination)
    ↓
VirtualService Routing (based on Host header + path)
    ├─ api.mybank.local/api/v1/auth/** → auth-service:8081
    ├─ api.mybank.local/api/v1/user/** → user-service:8085
    ├─ api.mybank.local/api/v1/pfm/** → pfm-core-service:8082
    ├─ api.mybank.local/api/v1/payment/** → payment-service:8083
    ├─ api.mybank.local/api/v1/invest/** → investment-service:8084
    ├─ api.mybank.local/** → api-gateway:8080
    └─ app.mybank.local/** → frontend:3000
```

### Service Communication:
```
auth-service
    ↓ (Feign Client)
user-service (UserServiceClient)
    ↓ (PostgreSQL)
postgres-user:5432/mybank_user
```

### Security Layers:
1. **TLS**: All traffic encrypted (self-signed cert)
2. **Istio mTLS**: Service-to-service encryption
3. **JWT**: Token-based authentication (future: API Gateway validation)
4. **Redis Sessions**: Global session management (future)
5. **Header Propagation**: X-User-Id, X-User-Email, X-User-Roles

## 🔜 Next Steps

### Immediate (To Complete Deployment):
1. ✅ auth-service and user-service are working
2. ⚠️ Fix business services (payment, investment, pfm-core)
3. ⚠️ Fix Kafka connectivity
4. ⚠️ Test end-to-end API flows

### API Gateway Integration (Not Yet Implemented):
- JWT validation at gateway layer
- Redis session validation
- Header injection for downstream services

Currently, VirtualService routes directly to services, bypassing API Gateway security filters.

### Testing & Validation:
- Register a new user
- Login and get JWT token
- Access protected endpoints
- Test inter-service communication (auth → user)
- Verify Redis session creation
- Test frontend authentication flow

## 📂 Modified Files in This Session

### Code Changes:
1. `auth-service/src/main/java/com/mybank/auth/AuthServiceApplication.java`
   - Excluded MongoAutoConfiguration
   - Excluded MicroserviceSecurityConfig via @ComponentScan filter

2. `user-service/src/main/java/com/mybank/user/UserServiceApplication.java`
   - Excluded MongoAutoConfiguration

3. `auth-service/Dockerfile`
   - Fixed JAR path to `auth-service/build/libs/auth-service-1.0.0-SNAPSHOT.jar`

### Configuration Changes:
4. `k8s/config/auth-service-configmap.yaml`
   - Changed database URL from `postgres:5432` to `postgres-auth:5432`

5. `k8s/config/user-service-configmap.yaml` (NEW)
   - Created configmap for user-service
   - Database URL: `postgres-user:5432`

6. `k8s/services/user-service.yaml`
   - Updated to use configMaps (common-config, user-service-config)
   - Added proper liveness/readiness probes

## 🎯 Success Criteria Met

- [x] Auth and user services separated with distinct databases
- [x] MongoDB autoconfiguration excluded from PostgreSQL-only services
- [x] Eureka discovery disabled (using Istio)
- [x] Both services running 2/2 (app + Istio sidecar)
- [x] PostgreSQL connections working
- [x] Docker images built and loaded to Kind
- [x] Kubernetes deployments updated
- [x] Configuration organized (common + service-specific)
- [ ] API endpoints accessible via HTTPS (blocked by local config)
- [ ] End-to-end authentication flow tested

## 📊 Performance & Health

### auth-service:
- Startup time: ~12.6 seconds
- Database: PostgreSQL (mybank_auth) - Connected ✅
- Redis: Connected ✅
- Actuator health: UP ✅

### user-service:
- Startup time: ~10-15 seconds (estimated)
- Database: PostgreSQL (mybank_user) - Connected ✅
- Redis: Connected ✅
- Actuator health: UP ✅

## 🔐 Security Configuration

### auth-service Security:
- Permits: `/api/auth/**`, `/actuator/**`
- Blocks: All other paths (403)
- Session: Stateless
- CSRF: Disabled
- Password encoder: BCrypt

### user-service Security:
- Uses HeaderAuthenticationFilter from common
- Extracts: X-User-Id, X-User-Email, X-User-Roles
- Sets Spring Security context
- Method-level authorization: @PreAuthorize

## 📅 Timeline

- **Session Start**: Fixed auth/user services
- **16:36**: Built and dockerized services
- **16:39-16:42**: Deployed to Kind, initial CrashLoopBackOff
- **16:43**: Fixed MongoDB autoconfiguration exclusion
- **16:44**: Fixed auth-service SecurityConfig conflict
- **16:46**: Both services running 2/2 ✅
- **16:47**: Current status - Ready for user testing

---

**Status**: Infrastructure deployed and healthy. Waiting for user to complete local configuration (hosts + certificate trust) to enable API testing.

**Deployment Cluster**: Kind (mybank-cluster)
**Namespace**: mybank
**Certificate Expiry**: 2035-11-01 (10 years)
**Port Forwarding**: localhost:8443→cluster:443, localhost:8080→cluster:80
