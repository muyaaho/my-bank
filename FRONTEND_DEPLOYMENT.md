# Frontend Deployment Guide

This guide explains how to deploy the MyBank frontend application.

## Architecture

The frontend is a Next.js 14 application that communicates with the backend microservices through the API Gateway.

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ http://localhost:30000 (Kind)
       │ http://localhost:3000 (Docker Compose/Local)
       │
┌──────▼──────────┐
│    Frontend     │ (Next.js)
│  (Port 3000)    │
└──────┬──────────┘
       │ http://api-gateway:8080
       │
┌──────▼──────────┐
│   API Gateway   │
│  (Port 8080)    │
└──────┬──────────┘
       │
       ├─> Auth Service (8081)
       ├─> PFM Core Service (8082)
       ├─> Payment Service (8083)
       └─> Investment Service (8084)
```

## Prerequisites

- Node.js 20+ (for local development)
- Docker (for containerization)
- Kubernetes/Kind (for K8s deployment)

## Local Development

### 1. Install Dependencies

First, fix npm cache permissions if needed:
```bash
sudo chown -R $(whoami) ~/.npm
```

Then install dependencies:
```bash
cd app
npm install
```

### 2. Environment Variables

Create `.env.local` file:
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3. Run Development Server

```bash
npm run dev
```

Access at: http://localhost:3000

## Docker Compose Deployment

### 1. Build and Start All Services

```bash
# Start infrastructure and backend services
docker-compose up -d postgres mongodb redis kafka

# Wait for services to be healthy
docker-compose ps

# Start backend services
docker-compose up -d service-discovery config-server api-gateway auth-service pfm-core-service payment-service investment-service

# Build and start frontend
docker-compose up -d frontend
```

### 2. Access Frontend

- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Grafana: http://localhost:3001

### 3. View Logs

```bash
docker-compose logs -f frontend
```

### 4. Stop Services

```bash
docker-compose down
```

## Kubernetes (Kind) Deployment

### 1. Build All Images

```bash
./build-images.sh
```

This builds:
- All backend microservices
- Frontend application

### 2. Deploy to Kind

```bash
# Complete deployment (build + deploy)
./kind-deploy-all.sh

# OR deploy only (if images already built)
./deploy-kind.sh
```

### 3. Verify Deployment

```bash
# Check pod status
kubectl get pods -n mybank

# Check frontend logs
kubectl logs -f deployment/frontend -n mybank

# Check services
kubectl get svc -n mybank
```

### 4. Access Frontend

Frontend is exposed via NodePort on port 30000:

```bash
# Access frontend
open http://localhost:30000

# Or use curl to test
curl http://localhost:30000
```

### 5. Troubleshooting

#### Frontend pod not starting

```bash
# Check pod status
kubectl describe pod -l app=frontend -n mybank

# Check logs
kubectl logs -l app=frontend -n mybank

# Common issues:
# 1. Image not loaded - run: kind load docker-image mybank/frontend:latest --name mybank-cluster
# 2. API Gateway not ready - check: kubectl get pods -n mybank | grep api-gateway
```

#### Cannot access frontend

```bash
# Verify service
kubectl get svc frontend -n mybank

# Port forward if NodePort not working
kubectl port-forward -n mybank svc/frontend 3000:3000

# Then access at http://localhost:3000
```

#### API calls failing

```bash
# Check API Gateway connectivity from frontend pod
kubectl exec -it deployment/frontend -n mybank -- wget -O- http://api-gateway:8080/actuator/health

# Check environment variables
kubectl exec -it deployment/frontend -n mybank -- env | grep NEXT_PUBLIC
```

## Build Process

### Docker Build Stages

The frontend uses a multi-stage Docker build for optimization:

1. **deps**: Install dependencies
2. **builder**: Build Next.js application
3. **runner**: Production runtime (minimal image)

### Image Size Optimization

- Uses Alpine Linux (smaller base image)
- Multi-stage build (only production files in final image)
- Standalone output (includes only necessary files)

```bash
# Check image size
docker images | grep mybank/frontend
```

## Testing the Deployment

### 1. Health Check

```bash
# Local
curl http://localhost:3000

# Kind
curl http://localhost:30000
```

### 2. Test Authentication Flow

1. Open frontend in browser
2. Click "회원가입" (Register)
3. Fill in the form:
   - Email: test@example.com
   - Name: 홍길동
   - Phone: 010-1234-5678
   - Password: Test1234! (must meet requirements)
4. Submit and verify redirect to dashboard

### 3. Test API Integration

After login, verify each page:
- Dashboard: Shows asset summary
- 지출 분석 (Spending): Shows spending breakdown
- 투자 (Investment): Shows investment summary
- 송금 (Payment): Can submit transfer request

### 4. Check Network Requests

Use browser DevTools (Network tab) to verify:
- API calls go to `/api/v1/*`
- JWT tokens are included in headers
- Responses are successful (200 status)

## Environment Configuration

### Production Environment Variables

```env
NEXT_PUBLIC_API_URL=http://api-gateway:8080
NODE_ENV=production
NEXT_TELEMETRY_DISABLED=1
```

### Docker Compose Environment

```yaml
environment:
  NEXT_PUBLIC_API_URL: http://localhost:8080
  NODE_ENV: production
```

### Kubernetes Environment

```yaml
env:
- name: NEXT_PUBLIC_API_URL
  value: "http://api-gateway:8080"
- name: NODE_ENV
  value: "production"
```

## Updating the Frontend

### 1. Make Changes

Edit files in `app/` directory.

### 2. Rebuild Image

```bash
# For Kind
./build-images.sh

# For Docker Compose
docker-compose build frontend
```

### 3. Redeploy

```bash
# Kind
kubectl rollout restart deployment/frontend -n mybank
kubectl rollout status deployment/frontend -n mybank

# Docker Compose
docker-compose up -d --force-recreate frontend
```

## Monitoring

### Logs

```bash
# Kind
kubectl logs -f deployment/frontend -n mybank

# Docker Compose
docker-compose logs -f frontend
```

### Resource Usage

```bash
# Kind
kubectl top pod -l app=frontend -n mybank
```

## Cleanup

### Docker Compose

```bash
docker-compose down
docker-compose down -v  # Also remove volumes
```

### Kind

```bash
./undeploy-kind.sh
```

## Production Considerations

### 1. Environment Variables

- Use secrets for sensitive data
- Configure CORS properly
- Set appropriate API URL for production

### 2. Security

- Enable HTTPS
- Configure CSP headers
- Implement rate limiting
- Use secure cookies

### 3. Performance

- Enable CDN for static assets
- Configure proper caching headers
- Use image optimization
- Enable compression

### 4. Monitoring

- Set up application monitoring (e.g., Sentry)
- Configure logging aggregation
- Set up uptime monitoring
- Track Web Vitals

## Common Issues

### Issue 1: npm install fails with permission error

**Solution:**
```bash
sudo chown -R $(whoami) ~/.npm
```

### Issue 2: Build fails in Docker

**Solution:**
Check Next.js configuration and ensure all dependencies are in package.json:
```bash
cd app
npm install
npm run build  # Test build locally first
```

### Issue 3: API calls return 401

**Solution:**
- Verify JWT token is being sent
- Check token expiration
- Verify API Gateway is running
- Check CORS configuration

### Issue 4: Frontend pod crashes

**Solution:**
```bash
kubectl logs deployment/frontend -n mybank
# Check for:
# - Missing environment variables
# - Memory limits too low
# - Build errors
```

## Support

For issues or questions:
1. Check logs (kubectl logs or docker-compose logs)
2. Verify all services are running (kubectl get pods or docker-compose ps)
3. Check network connectivity between services
4. Review environment variables configuration
