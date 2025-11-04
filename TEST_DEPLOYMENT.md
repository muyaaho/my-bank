# ✅ MyBank Deployment - Test Instructions

## 🎉 Deployment Status: COMPLETE

All services are running with HTTPS and self-signed certificates.

## 🔧 Required Setup

### 1. Trust CA Certificate (ONE TIME)

```bash
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  /Users/kimhyeonwoo/Documents/GitHub/mybank/certs/ca.crt
```

**Verify certificate is trusted:**
```bash
security find-certificate -c "MyBank Root CA" -a | grep "MyBank Root CA"
```

### 2. Add Hosts Entries (ONE TIME)

```bash
echo "127.0.0.1 mybank.local api.mybank.local app.mybank.local" | sudo tee -a /etc/hosts
```

**Verify hosts file:**
```bash
grep "mybank.local" /etc/hosts
```

### 3. Start Port Forwarding

**Terminal 1:**
```bash
cd /Users/kimhyeonwoo/Documents/GitHub/mybank
./port-forward.sh
```

Output should show:
```
🚀 Starting port forward for Istio Ingress Gateway...

📌 Access URLs:
  Frontend: https://app.mybank.local
  API: https://api.mybank.local

Forwarding from 127.0.0.1:443 -> 443
Forwarding from [::1]:443 -> 443
Forwarding from 127.0.0.1:80 -> 80
Forwarding from [::1]:80 -> 80
```

## 🧪 Testing

### Test 1: API Health Check

```bash
curl -k https://api.mybank.local/actuator/health
```

Expected: `{"status":"UP"}`

### Test 2: Register New User

```bash
curl -X POST https://api.mybank.local/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.local",
    "password": "password123",
    "name": "Test User",
    "phoneNumber": "010-1234-5678"
  }' \
  --cacert certs/ca.crt -v
```

Expected Response (200 OK):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid-here",
    "email": "test@mybank.local",
    "name": "Test User"
  }
}
```

### Test 3: Login

```bash
curl -X POST https://api.mybank.local/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@mybank.local",
    "password": "password123"
  }' \
  --cacert certs/ca.crt
```

Save the `accessToken` from response.

### Test 4: Access Protected Endpoint

```bash
# Replace YOUR_TOKEN_HERE with actual token from login
export TOKEN="YOUR_TOKEN_HERE"

curl https://api.mybank.local/api/v1/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  --cacert certs/ca.crt
```

Expected Response:
```json
{
  "id": "uuid",
  "email": "test@mybank.local",
  "name": "Test User",
  "phoneNumber": "010-1234-5678",
  "roles": ["USER"],
  "isActive": true,
  ...
}
```

### Test 5: Frontend Access

1. Open browser: **https://app.mybank.local**

2. You should see:
   - ✅ **NO certificate warning** (because CA is trusted)
   - ✅ MyBank frontend application
   - ✅ Green lock icon in address bar

3. Test login flow:
   - Enter credentials from Test 2
   - Should successfully log in
   - Should see dashboard

## 📊 Monitoring

### Check Pod Status

```bash
kubectl get pods -n mybank
```

All pods should show `2/2 Running` (app container + Istio sidecar):
```
NAME                                  READY   STATUS    RESTARTS   AGE
api-gateway-xxxx                      2/2     Running   0          Xm
auth-service-xxxx                     2/2     Running   0          Xm
user-service-xxxx                     2/2     Running   0          Xm  ← NEW
frontend-xxxx                         2/2     Running   0          Xm
postgres-auth-xxxx                    2/2     Running   0          Xm
postgres-user-xxxx                    2/2     Running   0          Xm
redis-xxxx                            2/2     Running   0          Xm
```

### View Logs

```bash
# User service logs
kubectl logs -f deployment/user-service -n mybank -c user-service

# Auth service logs
kubectl logs -f deployment/auth-service -n mybank -c auth-service

# API Gateway logs
kubectl logs -f deployment/api-gateway -n mybank -c api-gateway
```

### Check Redis Sessions

```bash
# Connect to Redis
kubectl exec -it deployment/redis -n mybank -- redis-cli

# List all session keys
KEYS mybank:*

# Get session for a user (replace USER_ID)
GET mybank:session:USER_ID

# Check blacklisted tokens
KEYS mybank:blacklist:*
```

### Check Istio Configuration

```bash
# Gateway
kubectl get gateway -n mybank
kubectl describe gateway mybank-gateway -n mybank

# VirtualServices
kubectl get virtualservice -n mybank
kubectl describe virtualservice mybank-api -n mybank

# Check TLS certificate
kubectl get secret mybank-tls-cert -n mybank
```

## 🐛 Troubleshooting

### Issue: Certificate Not Trusted

**Symptoms:** Browser shows "Not Secure" or "Your connection is not private"

**Solution:**
1. Trust CA certificate (see Setup step 1)
2. Restart browser completely
3. Clear browser SSL state (Chrome: `chrome://settings/clearBrowserData`)

### Issue: DNS Not Resolving

**Symptoms:** `curl: (6) Could not resolve host: api.mybank.local`

**Solution:**
```bash
# Check /etc/hosts
cat /etc/hosts | grep mybank

# Should show:
127.0.0.1 mybank.local api.mybank.local app.mybank.local

# If not, add it:
echo "127.0.0.1 mybank.local api.mybank.local app.mybank.local" | sudo tee -a /etc/hosts
```

### Issue: Connection Refused

**Symptoms:** `curl: (7) Failed to connect to api.mybank.local port 443`

**Solution:**
1. Check port forwarding is running:
```bash
ps aux | grep "port-forward"
```

2. Restart port forwarding:
```bash
pkill -f "kubectl port-forward"
./port-forward.sh
```

### Issue: 401 Unauthorized

**Symptoms:** API returns `{"error":"unauthorized"}`

**Solution:**
1. JWT token expired - login again
2. Token blacklisted - login again
3. Redis session expired - login again

### Issue: Services Not Ready

**Symptoms:** Pods stuck in `0/2` or `CrashLoopBackOff`

**Solution:**
```bash
# Check pod details
kubectl describe pod POD_NAME -n mybank

# Check logs
kubectl logs POD_NAME -n mybank --all-containers

# Common fixes:
# - Database not ready: Wait for postgres/mongodb pods
# - Image pull error: Reload images to Kind
# - Config error: Check environment variables
```

## 🎯 What's Working

✅ **HTTPS with Self-Signed Certificates**
- Wildcard cert for `*.mybank.local`
- TLS termination at Istio Gateway
- HTTP → HTTPS redirect

✅ **Auth/User Service Separation**
- `auth-service`: Authentication only
- `user-service`: User profile management
- Feign client communication

✅ **Enhanced Security**
- JWT validation at API Gateway
- Redis session validation
- Token blacklist on logout
- Sliding window session expiration (30 min)
- Header-based auth in microservices

✅ **Service Mesh (Istio)**
- Traffic management
- mTLS between services
- Observability (via sidecar)

✅ **Infrastructure**
- PostgreSQL (auth + user databases)
- MongoDB (business data)
- Redis (sessions + cache)
- Kafka (event streaming)

## 📈 Performance

Expected response times (local Kind):
- Health check: < 50ms
- Registration: < 500ms
- Login: < 300ms
- Protected API: < 200ms
- Frontend load: < 1s

## 🚀 Next Steps

1. ✅ Complete setup (Trust CA + hosts)
2. ✅ Start port forwarding
3. ✅ Test API endpoints
4. ✅ Test frontend access
5. 📊 Monitor logs and metrics
6. 🧪 Load testing (optional)
7. 🔐 Production certificate (Let's Encrypt)

## 📚 Related Docs

- [Architecture Refactoring Summary](./ARCHITECTURE_REFACTORING_SUMMARY.md)
- [Deployment Complete Guide](./DEPLOYMENT_COMPLETE.md)
- [Certificate README](./certs/README.md)

---

**Test Date**: 2025-11-03
**Cluster**: Kind (mybank-cluster)
**Namespace**: mybank
**Certificate Expiry**: 2035-11-01 (10 years)
