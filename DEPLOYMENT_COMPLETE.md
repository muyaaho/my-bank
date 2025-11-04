# 🎉 MyBank Deployment Complete - HTTPS with Self-Signed Certificates

## ✅ Deployment Status

### Completed Steps

1. ✅ **Generated Self-Signed Certificates**
   - Location: `certs/`
   - Wildcard certificate for `*.mybank.local`
   - Valid for 10 years

2. ✅ **Kubernetes Secret Created**
   - Secret name: `mybank-tls-cert`
   - Namespace: `mybank`

3. ✅ **Infrastructure Deployed**
   - PostgreSQL (auth) - `postgres-auth`
   - PostgreSQL (user) - `postgres-user`
   - MongoDB - `mongodb`
   - Redis - `redis`

4. ✅ **Microservices Deployed**
   - `auth-service` - Authentication (Port 8081)
   - `user-service` - User management (Port 8085) **NEW**
   - `api-gateway` - Gateway (Port 8080)
   - `pfm-core-service` - PFM (Port 8082)
   - `payment-service` - Payments (Port 8083)
   - `investment-service` - Investments (Port 8084)

5. ✅ **Istio Gateway Configured**
   - HTTPS on port 443
   - HTTP redirect to HTTPS on port 80
   - TLS with `mybank-tls-cert`

6. ✅ **VirtualServices Configured**
   - Frontend: `app.mybank.local`
   - API: `api.mybank.local`

## 🔧 Setup Instructions

### 1. Trust CA Certificate

```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /Users/kimhyeonwoo/Documents/GitHub/mybank/certs/ca.crt
```

### 2. Update /etc/hosts

```bash
echo "127.0.0.1 mybank.local api.mybank.local app.mybank.local" | sudo tee -a /etc/hosts
```

### 3. Port Forward Istio Ingress Gateway

**Terminal 1 (HTTPS):**
```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 443:443
```

**Terminal 2 (HTTP):**
```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 80:80
```

## 🌐 Access URLs

- **Frontend**: https://app.mybank.local
- **API Gateway**: https://api.mybank.local
- **Auth API**: https://api.mybank.local/api/v1/auth
- **User API**: https://api.mybank.local/api/v1/user

## 🧪 Test Authentication Flow

### 1. Register a New User

```bash
curl -X POST https://api.mybank.local/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.local",
    "password": "password123",
    "name": "Test User",
    "phoneNumber": "010-1234-5678"
  }' \
  --cacert certs/ca.crt
```

Expected response:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "...",
    "email": "test@mybank.local",
    "name": "Test User"
  }
}
```

### 2. Login

```bash
curl -X POST https://api.mybank.local/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.local",
    "password": "password123"
  }' \
  --cacert certs/ca.crt
```

### 3. Access Protected Endpoint (Get User Profile)

```bash
export TOKEN="<access_token_from_login>"

curl https://api.mybank.local/api/v1/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  --cacert certs/ca.crt
```

### 4. Test Frontend

Open browser: https://app.mybank.local

If you see certificate warning:
1. Click "Advanced"
2. Click "Proceed to app.mybank.local" (because you trusted the CA)

## 📊 Monitoring

### Check Pod Status

```bash
kubectl get pods -n mybank
```

Expected output:
```
NAME                                  READY   STATUS    RESTARTS   AGE
api-gateway-xxxx                      2/2     Running   0          Xm
auth-service-xxxx                     2/2     Running   0          Xm
user-service-xxxx                     2/2     Running   0          Xm
pfm-core-service-xxxx                 2/2     Running   0          Xm
payment-service-xxxx                  2/2     Running   0          Xm
investment-service-xxxx               2/2     Running   0          Xm
postgres-auth-xxxx                    2/2     Running   0          Xm
postgres-user-xxxx                    2/2     Running   0          Xm
mongodb-xxxx                          2/2     Running   0          Xm
redis-xxxx                            2/2     Running   0          Xm
frontend-xxxx                         2/2     Running   0          Xm
```

### View Logs

```bash
# Auth service
kubectl logs -f deployment/auth-service -n mybank -c auth-service

# User service
kubectl logs -f deployment/user-service -n mybank -c user-service

# API Gateway
kubectl logs -f deployment/api-gateway -n mybank -c api-gateway
```

### Check Istio Gateway

```bash
kubectl get gateway -n mybank
kubectl get virtualservice -n mybank
```

## 🔍 Troubleshooting

### Certificate Not Trusted

If browser shows "Not Secure":
1. Ensure CA certificate is trusted (Step 1 above)
2. Restart browser
3. Clear browser cache

### Services Not Starting

Check logs:
```bash
kubectl describe pod <pod-name> -n mybank
kubectl logs <pod-name> -n mybank
```

Common issues:
- Database not ready: Wait for PostgreSQL/MongoDB pods to be Running
- Image not found: Check Kind image load completed
- Port conflicts: Ensure no other services on ports 80/443

### Port Forward Not Working

```bash
# Check Istio ingress gateway is running
kubectl get pods -n istio-system | grep ingressgateway

# Restart port forward
pkill -f "kubectl port-forward"
kubectl port-forward -n istio-system svc/istio-ingressgateway 443:443 80:80
```

### Redis Session Issues

Check Redis is running:
```bash
kubectl exec -it deployment/redis -n mybank -- redis-cli ping
```

Check session keys:
```bash
kubectl exec -it deployment/redis -n mybank -- redis-cli KEYS "mybank:*"
```

## 🎯 Architecture Highlights

### Security Layers

1. **HTTPS Only** - All traffic encrypted with TLS
2. **JWT Validation** - API Gateway validates JWT tokens
3. **Redis Session Check** - Validates active sessions and blacklist
4. **Header-based Auth** - Microservices authenticate via headers from gateway

### Flow

```
Browser (HTTPS)
    ↓
Istio Ingress Gateway (443)
    ↓
API Gateway (Spring Security WebFlux)
    ├─ JwtAuthenticationWebFilter
    │   ├─ Validate JWT signature/expiration
    │   └─ Extract user claims
    └─ RedisSessionWebFilter
        ├─ Check token not blacklisted
        ├─ Validate session exists
        └─ Refresh session TTL
    ↓
Add Headers: X-User-Id, X-User-Email, X-User-Roles
    ↓
Microservices (Header Auth)
    └─ HeaderAuthenticationFilter
        └─ Set SecurityContext
```

## 📈 Next Steps

1. **Trust CA Certificate** - Required for HTTPS access
2. **Test Authentication** - Register → Login → Access API
3. **Frontend Testing** - Open https://app.mybank.local
4. **Monitor Logs** - Check services are healthy
5. **Load Testing** - Use k6 or similar tools

## 🚀 Quick Commands

```bash
# Check everything is running
kubectl get all -n mybank

# Restart a service
kubectl rollout restart deployment/auth-service -n mybank

# Scale a service
kubectl scale deployment/user-service --replicas=3 -n mybank

# Port forward (all-in-one)
kubectl port-forward -n istio-system svc/istio-ingressgateway 443:443 80:80

# View real-time logs (all services)
stern -n mybank .

# Test API health
curl -k https://api.mybank.local/actuator/health
```

## 📚 Documentation

- [Architecture Refactoring Summary](./ARCHITECTURE_REFACTORING_SUMMARY.md)
- [Certificate README](./certs/README.md)
- [Claude AI Instructions](./CLAUDE.md)

---

**Deployment Date**: 2025-11-03
**Platform**: Kind Kubernetes Cluster
**TLS**: Self-signed certificates (10-year validity)
**Architecture**: Microservices + Event-Driven + Service Mesh (Istio)
